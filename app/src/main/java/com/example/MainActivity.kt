package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CrosshairPreferences
import com.example.service.DotOverlayService
import com.example.ui.DotCrosshairScreen
import com.example.ui.theme.DotCrosshairTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var preferences: CrosshairPreferences
    private var hasOverlayPermission by mutableStateOf(false)
    private var isBatteryOptimizationIgnored by mutableStateOf(false)

    // Interactive in-app live preview gyro offsets
    private var previewOffsetX by mutableFloatStateOf(0f)
    private var previewOffsetY by mutableFloatStateOf(0f)

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var currentLastPitch = 0f
    private var currentLastRoll = 0f

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled silently
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = CrosshairPreferences(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        checkPermissionsAndStatus()
        checkNotificationPermission()

        setContent {
            DotCrosshairTheme {
                val state by preferences.state.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DotCrosshairScreen(
                        state = state,
                        hasOverlayPermission = hasOverlayPermission,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        previewOffsetX = previewOffsetX,
                        previewOffsetY = previewOffsetY,
                        onToggleEnabled = { enabled ->
                            if (enabled && !hasOverlayPermission) {
                                requestOverlayPermission()
                            } else {
                                preferences.setEnabled(enabled)
                                if (enabled) {
                                    DotOverlayService.start(this@MainActivity)
                                } else {
                                    DotOverlayService.stop(this@MainActivity)
                                }
                            }
                        },
                        onSizeChange = { sizeDp ->
                            preferences.setSizeDp(sizeDp)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onColorChange = { colorArgb ->
                            preferences.setColor(colorArgb)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onToggleGyro = { enabled ->
                            preferences.setGyroEnabled(enabled)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                            updateInAppSensorListener(enabled)
                        },
                        onGyroSensitivityChange = { sensitivity ->
                            preferences.setGyroSensitivity(sensitivity)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onGyroSmoothnessChange = { smoothness ->
                            preferences.setGyroSmoothness(smoothness)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onToggleInvertX = { invert ->
                            preferences.setGyroInvertX(invert)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onToggleInvertY = { invert ->
                            preferences.setGyroInvertY(invert)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onCalibrateGyro = {
                            preferences.calibrateNeutral(currentLastPitch, currentLastRoll)
                            previewOffsetX = 0f
                            previewOffsetY = 0f
                            if (state.isEnabled) {
                                DotOverlayService.calibrate(this@MainActivity)
                            }
                        },
                        onRequestPermission = {
                            requestOverlayPermission()
                        },
                        onRequestDisableBatteryOptimization = {
                            requestDisableBatteryOptimization()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStatus()
        if (!hasOverlayPermission && preferences.state.value.isEnabled) {
            preferences.setEnabled(false)
            DotOverlayService.stop(this)
        }
        updateInAppSensorListener(preferences.state.value.isGyroEnabled)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun updateInAppSensorListener(enabled: Boolean) {
        val sm = sensorManager
        val sensor = rotationSensor
        if (enabled && sm != null && sensor != null) {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sm?.unregisterListener(this)
            previewOffsetX = 0f
            previewOffsetY = 0f
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val state = preferences.state.value
        if (!state.isGyroEnabled) return

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitch = orientation[1]
                val roll = orientation[2]
                currentLastPitch = pitch
                currentLastRoll = roll

                var deltaRoll = roll - state.neutralRoll
                var deltaPitch = pitch - state.neutralPitch

                val deadzone = 0.015f
                if (kotlin.math.abs(deltaRoll) < deadzone) deltaRoll = 0f
                if (kotlin.math.abs(deltaPitch) < deadzone) deltaPitch = 0f

                val scale = 80f * state.gyroSensitivity
                val targetX = deltaRoll * scale * (if (state.gyroInvertX) -1f else 1f)
                val targetY = deltaPitch * scale * (if (state.gyroInvertY) -1f else 1f)

                val alpha = (1f - state.gyroSmoothness).coerceIn(0.1f, 1f)
                previewOffsetX += (targetX.coerceIn(-50f, 50f) - previewOffsetX) * alpha
                previewOffsetY += (targetY.coerceIn(-40f, 40f) - previewOffsetY) * alpha
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val accelX = -event.values[0] / 9.81f
                val accelY = event.values[1] / 9.81f

                val scale = 50f * state.gyroSensitivity
                val targetX = (accelX - state.neutralRoll) * scale * (if (state.gyroInvertX) -1f else 1f)
                val targetY = (accelY - state.neutralPitch) * scale * (if (state.gyroInvertY) -1f else 1f)

                val alpha = (1f - state.gyroSmoothness).coerceIn(0.1f, 1f)
                previewOffsetX += (targetX.coerceIn(-50f, 50f) - previewOffsetX) * alpha
                previewOffsetY += (targetY.coerceIn(-40f, 40f) - previewOffsetY) * alpha
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun checkPermissionsAndStatus() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun requestOverlayPermission() {
        if (!hasOverlayPermission) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestDisableBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            } catch (_: Exception) {
                // If device manufacturer removed the intent, gracefully ignore
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
