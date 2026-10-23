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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CrosshairPreferences
import com.example.service.DotOverlayService
import com.example.service.GyroAccessibilityService
import com.example.ui.DotCrosshairScreen
import com.example.ui.theme.DotCrosshairTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var preferences: CrosshairPreferences
    private var hasOverlayPermission by mutableStateOf(false)
    private var isBatteryOptimizationIgnored by mutableStateOf(false)
    private var isAccessibilityEnabled by mutableStateOf(false)

    // Current detected direction label for live user feedback
    private var detectedDirectionLabel by mutableStateOf("Neutro (Centro)")

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var currentLastPitch = 0f
    private var currentLastRoll = 0f

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Handled silently
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
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        detectedDirectionLabel = detectedDirectionLabel,
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
                        onGyroStepChange = { stepPx ->
                            preferences.setGyroStepPx(stepPx)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onGyroIntervalChange = { intervalMs ->
                            preferences.setGyroIntervalMs(intervalMs)
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
                            detectedDirectionLabel = "Neutro (Calibrado)"
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
                        onRequestAccessibility = {
                            openAccessibilitySettings()
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
            detectedDirectionLabel = "Neutro (Centro)"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val state = preferences.state.value
        if (!state.isGyroEnabled) return

        var deltaRoll = 0f
        var deltaPitch = 0f

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitch = orientation[1]
                val roll = orientation[2]
                currentLastPitch = pitch
                currentLastRoll = roll

                deltaRoll = roll - state.neutralRoll
                deltaPitch = pitch - state.neutralPitch
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val accelX = -event.values[0] / 9.81f
                val accelY = event.values[1] / 9.81f
                currentLastPitch = accelY
                currentLastRoll = accelX

                deltaRoll = accelX - state.neutralRoll
                deltaPitch = accelY - state.neutralPitch
            }
        }

        val dirX = if (state.gyroInvertX) -deltaRoll else deltaRoll
        val dirY = if (state.gyroInvertY) -deltaPitch else deltaPitch

        val magnitude = kotlin.math.hypot(dirX.toDouble(), dirY.toDouble()).toFloat()

        if (magnitude < 0.035f) {
            detectedDirectionLabel = "Neutro (Centro)"
            return
        }

        val angle = kotlin.math.atan2(dirY.toDouble(), dirX.toDouble())
        val normalizedAngle = if (angle < 0) angle + (2.0 * Math.PI) else angle
        val sector = (((normalizedAngle + (Math.PI / 8.0)) / (Math.PI / 4.0)).toInt()) % 8

        detectedDirectionLabel = when (sector) {
            0 -> "Leste (Direita →)"
            1 -> "Sudeste (Direita + Baixo ↘)"
            2 -> "Sul (Baixo ↓)"
            3 -> "Sudoeste (Esquerda + Baixo ↙)"
            4 -> "Oeste (Esquerda ←)"
            5 -> "Noroeste (Esquerda + Cima ↖)"
            6 -> "Norte (Cima ↑)"
            7 -> "Nordeste (Direita + Cima ↗)"
            else -> "Neutro (Centro)"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun checkPermissionsAndStatus() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(packageName) == true
        isAccessibilityEnabled = GyroAccessibilityService.isRunning()
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
                // Ignore gracefully
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            // Ignore gracefully
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
