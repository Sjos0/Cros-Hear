package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CrosshairState(
    val isEnabled: Boolean = false,
    val dotSizeDp: Int = 6,
    val dotColorArgb: Int = 0xFF00FF66.toInt(),
    val isGyroEnabled: Boolean = false,
    val gyroSensitivity: Float = 1.0f,
    val gyroStepPx: Int = 45,
    val gyroIntervalMs: Long = 75L,
    val gyroInvertX: Boolean = false,
    val gyroInvertY: Boolean = false,
    val neutralPitch: Float = 0f,
    val neutralRoll: Float = 0f
)

class CrosshairPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadCurrentState())
    val state: StateFlow<CrosshairState> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _state.value = loadCurrentState()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun loadCurrentState(): CrosshairState {
        return CrosshairState(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            dotSizeDp = prefs.getInt(KEY_SIZE_DP, DEFAULT_SIZE_DP),
            dotColorArgb = prefs.getInt(KEY_COLOR, DEFAULT_COLOR),
            isGyroEnabled = prefs.getBoolean(KEY_GYRO_ENABLED, false),
            gyroSensitivity = prefs.getFloat(KEY_GYRO_SENSITIVITY, DEFAULT_GYRO_SENSITIVITY),
            gyroStepPx = prefs.getInt(KEY_GYRO_STEP_PX, DEFAULT_GYRO_STEP_PX),
            gyroIntervalMs = prefs.getLong(KEY_GYRO_INTERVAL_MS, DEFAULT_GYRO_INTERVAL_MS),
            gyroInvertX = prefs.getBoolean(KEY_GYRO_INVERT_X, false),
            gyroInvertY = prefs.getBoolean(KEY_GYRO_INVERT_Y, false),
            neutralPitch = prefs.getFloat(KEY_NEUTRAL_PITCH, 0f),
            neutralRoll = prefs.getFloat(KEY_NEUTRAL_ROLL, 0f)
        )
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setSizeDp(sizeDp: Int) {
        val clamped = sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
        prefs.edit().putInt(KEY_SIZE_DP, clamped).apply()
        _state.value = _state.value.copy(dotSizeDp = clamped)
    }

    fun setColor(colorArgb: Int) {
        prefs.edit().putInt(KEY_COLOR, colorArgb).apply()
        _state.value = _state.value.copy(dotColorArgb = colorArgb)
    }

    fun setGyroEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isGyroEnabled = enabled)
    }

    fun setGyroSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(MIN_GYRO_SENSITIVITY, MAX_GYRO_SENSITIVITY)
        prefs.edit().putFloat(KEY_GYRO_SENSITIVITY, clamped).apply()
        _state.value = _state.value.copy(gyroSensitivity = clamped)
    }

    fun setGyroStepPx(stepPx: Int) {
        val clamped = stepPx.coerceIn(15, 120)
        prefs.edit().putInt(KEY_GYRO_STEP_PX, clamped).apply()
        _state.value = _state.value.copy(gyroStepPx = clamped)
    }

    fun setGyroIntervalMs(intervalMs: Long) {
        val clamped = intervalMs.coerceIn(40L, 160L)
        prefs.edit().putLong(KEY_GYRO_INTERVAL_MS, clamped).apply()
        _state.value = _state.value.copy(gyroIntervalMs = clamped)
    }

    fun setGyroInvertX(invert: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_INVERT_X, invert).apply()
        _state.value = _state.value.copy(gyroInvertX = invert)
    }

    fun setGyroInvertY(invert: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_INVERT_Y, invert).apply()
        _state.value = _state.value.copy(gyroInvertY = invert)
    }

    fun calibrateNeutral(pitch: Float, roll: Float) {
        prefs.edit()
            .putFloat(KEY_NEUTRAL_PITCH, pitch)
            .putFloat(KEY_NEUTRAL_ROLL, roll)
            .apply()
        _state.value = _state.value.copy(neutralPitch = pitch, neutralRoll = roll)
    }

    companion object {
        const val PREFS_NAME = "dot_crosshair_prefs"
        const val KEY_ENABLED = "key_dot_enabled"
        const val KEY_SIZE_DP = "key_dot_size_dp"
        const val KEY_COLOR = "key_dot_color"

        const val KEY_GYRO_ENABLED = "key_gyro_enabled"
        const val KEY_GYRO_SENSITIVITY = "key_gyro_sensitivity"
        const val KEY_GYRO_STEP_PX = "key_gyro_step_px"
        const val KEY_GYRO_INTERVAL_MS = "key_gyro_interval_ms"
        const val KEY_GYRO_INVERT_X = "key_gyro_invert_x"
        const val KEY_GYRO_INVERT_Y = "key_gyro_invert_y"
        const val KEY_NEUTRAL_PITCH = "key_neutral_pitch"
        const val KEY_NEUTRAL_ROLL = "key_neutral_roll"

        const val DEFAULT_SIZE_DP = 6
        const val MIN_SIZE_DP = 2
        const val MAX_SIZE_DP = 24
        val DEFAULT_COLOR = 0xFF00FF66.toInt() // Neon Green

        const val DEFAULT_GYRO_SENSITIVITY = 1.0f
        const val MIN_GYRO_SENSITIVITY = 0.2f
        const val MAX_GYRO_SENSITIVITY = 3.0f

        const val DEFAULT_GYRO_STEP_PX = 45
        const val DEFAULT_GYRO_INTERVAL_MS = 75L
    }
}
