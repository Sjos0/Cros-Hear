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
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CrosshairPreferences

class DotOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var dotView: DotView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var preferences: CrosshairPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = CrosshairPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlayService()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            preferences.setEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        showOrUpdateOverlay()

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

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            val params = WindowManager.LayoutParams(
                diameterPx,
                diameterPx,
                windowType,
                flags,
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
            val currentParams = layoutParams
            if (currentParams != null && (currentParams.width != diameterPx || currentParams.height != diameterPx)) {
                currentParams.width = diameterPx
                currentParams.height = diameterPx
                try {
                    wm.updateViewLayout(dotView, currentParams)
                } catch (_: Exception) {
                    // Ignored if view is in transition
                }
            }
        }
    }

    private fun stopOverlayService() {
        preferences.setEnabled(false)
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
    }
}
