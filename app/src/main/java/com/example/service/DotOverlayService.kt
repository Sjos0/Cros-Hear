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
import android.os.SystemClock
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

    // Pre-allocated arrays to eliminate GC overhead in sensor callbacks
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Current tilt angles cached for calibration
    private var lastPitch = 0f
    private var lastRoll = 0f

    // Cooldown management so gyro drag gestures do not compete with user's manual touches
    private var lastSwipeUptime = 0L

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

            // Small centered non-focusable and non-touchable overlay window
            // Dot stays 100% mathematically centered on the screen at all times
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
                gravity = Gravity.CENTER
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
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val state = preferences.state.value
        if (!state.isEnabled || !state.isGyroEnabled) return

        var deltaRoll = 0f
        var deltaPitch = 0f

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitch = orientation[1]
                val roll = orientation[2]
                lastPitch = pitch
                lastRoll = roll

                deltaRoll = roll - state.neutralRoll
                deltaPitch = pitch - state.neutralPitch
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val accelX = -event.values[0] / 9.81f
                val accelY = event.values[1] / 9.81f
                lastPitch = accelY
                lastRoll = accelX

                deltaRoll = accelX - state.neutralRoll
                deltaPitch = accelY - state.neutralPitch
            }
        }

        // Apply axis inversion
        val dirX = if (state.gyroInvertX) -deltaRoll else deltaRoll
        val dirY = if (state.gyroInvertY) -deltaPitch else deltaPitch

        val magnitude = kotlin.math.hypot(dirX.toDouble(), dirY.toDouble()).toFloat()

        // Deadzone threshold (subtle hand movements do not trigger drag)
        val deadzone = 0.035f
        if (magnitude < deadzone) return

        // Throttle gesture dispatches to prevent competing with user's manual screen touches
        val now = SystemClock.uptimeMillis()
        if (now - lastSwipeUptime < state.gyroIntervalMs) return

        // Quantize direction into the 8 discrete cardinal and intercardinal sectors
        // 0: Leste (Direita), 1: Sudeste, 2: Sul (Baixo), 3: Sudoeste,
        // 4: Oeste (Esquerda), 5: Noroeste, 6: Norte (Cima), 7: Nordeste
        val angle = kotlin.math.atan2(dirY.toDouble(), dirX.toDouble())
        val normalizedAngle = if (angle < 0) angle + (2.0 * Math.PI) else angle
        val sector = (((normalizedAngle + (Math.PI / 8.0)) / (Math.PI / 4.0)).toInt()) % 8

        val (unitX, unitY) = when (sector) {
            0 -> 1.0f to 0.0f                 // Leste (Direita)
            1 -> 0.7071f to 0.7071f           // Sudeste (Baixo + Direita)
            2 -> 0.0f to 1.0f                 // Sul (Baixo)
            3 -> -0.7071f to 0.7071f          // Sudoeste (Baixo + Esquerda)
            4 -> -1.0f to 0.0f                // Oeste (Esquerda)
            5 -> -0.7071f to -0.7071f         // Noroeste (Cima + Esquerda)
            6 -> 0.0f to -1.0f                // Norte (Cima)
            7 -> 0.7071f to -0.7071f          // Nordeste (Cima + Direita)
            else -> 0.0f to 0.0f
        }

        // Compute step distance proportional to tilt intensity and sensitivity setting
        val dynamicScale = (magnitude / 0.15f).coerceIn(0.6f, 2.2f)
        val step = (state.gyroStepPx * state.gyroSensitivity * dynamicScale).toInt()

        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        // Anchor touch coordinates in typical camera control area (right side of screen)
        val startX = screenW * 0.70f
        val startY = screenH * 0.55f
        val endX = startX + (unitX * step)
        val endY = startY + (unitY * step)

        // Dispatch simulated touch swipe via AccessibilityService
        val dispatched = GyroAccessibilityService.dispatchSwipe(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            durationMs = 30
        )

        if (dispatched) {
            lastSwipeUptime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun calibrateCurrentPosition() {
        preferences.calibrateNeutral(lastPitch, lastRoll)
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
