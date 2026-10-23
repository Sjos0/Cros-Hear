package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CrosshairPreferences

class DotOverlayService : Service(), SensorEventListener {

    private var windowManager: WindowManager? = null
    private var dotView: DotView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var preferences: CrosshairPreferences

    private var sensorManager: SensorManager? = null
    private var activeSensor: Sensor? = null
    private var isSensorRegistered = false

    // Pre-allocated arrays to avoid GC allocations in the sensor loop
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Filter and smoothing variables
    private var currentFilteredX = 0f
    private var currentFilteredY = 0f

    // Current tilt angles cached for calibration
    private var lastPitch = 0f
    private var lastRoll = 0f

    override fun onCreate() {
        super.onCreate()
        preferences = CrosshairPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        setupSensors()
        createNotificationChannel()
    }

    private fun setupSensors() {
        val sm = sensorManager ?: return
        activeSensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlayService()
                return START_NOT_STICKY
            }
            ACTION_CALIBRATE -> {
                calibrateCurrentPosition()
                return START_STICKY
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            preferences.setEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        showOrUpdateOverlay()
        updateSensorRegistration()

        return START_STICKY
    }

    private fun showOrUpdateOverlay() {
        val wm = windowManager ?: return
        val state = preferences.state.value

        val density = resources.displayMetrics.density
        val diameterPx = (state.dotSizeDp * density).toInt().coerceAtLeast(2)
        val radiusPx = diameterPx / 2f

        if (dotView == null) {
            val view = DotView(this)
            view.update(radiusPx, state.dotColorArgb)

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Using MATCH_PARENT with FLAG_NOT_TOUCHABLE allows zero-latency GPU rendering
            // of the dynamic gyro offset without calling windowManager.updateViewLayout()
            val windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                windowFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            try {
                wm.addView(view, params)
                dotView = view
                layoutParams = params
            } catch (_: Exception) {
                preferences.setEnabled(false)
                stopSelf()
            }
        } else {
            dotView?.update(radiusPx, state.dotColorArgb)
        }
    }

    private fun updateSensorRegistration() {
        val state = preferences.state.value
        val sm = sensorManager
        val sensor = activeSensor

        if (state.isEnabled && state.isGyroEnabled && sm != null && sensor != null) {
            if (!isSensorRegistered) {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
                isSensorRegistered = true
            }
        } else {
            if (isSensorRegistered) {
                sm?.unregisterListener(this)
                isSensorRegistered = false
                currentFilteredX = 0f
                currentFilteredY = 0f
                dotView?.resetOffset()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val state = preferences.state.value
        if (!state.isEnabled || !state.isGyroEnabled) return

        val view = dotView ?: return

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val pitch = orientation[1] // Tilt forward/backward
                val roll = orientation[2]  // Tilt left/right
                lastPitch = pitch
                lastRoll = roll

                var deltaRoll = roll - state.neutralRoll
                var deltaPitch = pitch - state.neutralPitch

                // Deadzone to prevent natural resting hand shake
                val deadzone = 0.015f
                if (kotlin.math.abs(deltaRoll) < deadzone) deltaRoll = 0f
                if (kotlin.math.abs(deltaPitch) < deadzone) deltaPitch = 0f

                val screenDim = minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels).toFloat()
                val scale = screenDim * 0.85f * state.gyroSensitivity

                val targetX = deltaRoll * scale * (if (state.gyroInvertX) -1f else 1f)
                val targetY = deltaPitch * scale * (if (state.gyroInvertY) -1f else 1f)

                val maxRange = screenDim * 0.45f
                val clampedX = targetX.coerceIn(-maxRange, maxRange)
                val clampedY = targetY.coerceIn(-maxRange, maxRange)

                val alpha = (1f - state.gyroSmoothness).coerceIn(0.08f, 1f)
                currentFilteredX += (clampedX - currentFilteredX) * alpha
                currentFilteredY += (clampedY - currentFilteredY) * alpha

                view.setOffset(currentFilteredX, currentFilteredY)
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Gyroscope angular rate (rad/s)
                val rateX = event.values[0]
                val rateY = event.values[1]

                val targetX = rateY * 40f * state.gyroSensitivity * (if (state.gyroInvertX) -1f else 1f)
                val targetY = rateX * 40f * state.gyroSensitivity * (if (state.gyroInvertY) -1f else 1f)

                val alpha = (1f - state.gyroSmoothness).coerceIn(0.1f, 1f)
                currentFilteredX += (targetX - currentFilteredX) * alpha
                currentFilteredY += (targetY - currentFilteredY) * alpha

                view.setOffset(currentFilteredX, currentFilteredY)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val accelX = -event.values[0] / 9.81f
                val accelY = event.values[1] / 9.81f

                val screenDim = minOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels).toFloat()
                val scale = screenDim * 0.5f * state.gyroSensitivity

                val targetX = (accelX - state.neutralRoll) * scale * (if (state.gyroInvertX) -1f else 1f)
                val targetY = (accelY - state.neutralPitch) * scale * (if (state.gyroInvertY) -1f else 1f)

                val alpha = (1f - state.gyroSmoothness).coerceIn(0.1f, 1f)
                currentFilteredX += (targetX - currentFilteredX) * alpha
                currentFilteredY += (targetY - currentFilteredY) * alpha

                view.setOffset(currentFilteredX, currentFilteredY)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed for accuracy adjustments
    }

    private fun calibrateCurrentPosition() {
        preferences.calibrateNeutral(lastPitch, lastRoll)
        currentFilteredX = 0f
        currentFilteredY = 0f
        dotView?.resetOffset()
    }

    private fun stopOverlayService() {
        preferences.setEnabled(false)
        if (isSensorRegistered) {
            sensorManager?.unregisterListener(this)
            isSensorRegistered = false
        }
        removeOverlayView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun removeOverlayView() {
        dotView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
                // View might already be detached
            }
            dotView = null
            layoutParams = null
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DotOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (isSensorRegistered) {
            sensorManager?.unregisterListener(this)
            isSensorRegistered = false
        }
        removeOverlayView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "dot_crosshair_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE = "com.example.action.UPDATE"
        const val ACTION_CALIBRATE = "com.example.action.CALIBRATE"

        fun start(context: Context) {
            val intent = Intent(context, DotOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DotOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, DotOverlayService::class.java).apply {
                action = ACTION_UPDATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun calibrate(context: Context) {
            val intent = Intent(context, DotOverlayService::class.java).apply {
                action = ACTION_CALIBRATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
