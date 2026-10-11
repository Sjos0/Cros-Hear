package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CrosshairState(
    val isEnabled: Boolean = false,
    val dotSizeDp: Int = 6,
    val dotColorArgb: Int = 0xFF00FF66.toInt()
)

class CrosshairPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        CrosshairState(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            dotSizeDp = prefs.getInt(KEY_SIZE_DP, DEFAULT_SIZE_DP),
            dotColorArgb = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        )
    )
    val state: StateFlow<CrosshairState> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_ENABLED, KEY_SIZE_DP, KEY_COLOR -> {
                _state.value = CrosshairState(
                    isEnabled = prefs.getBoolean(KEY_ENABLED, false),
                    dotSizeDp = prefs.getInt(KEY_SIZE_DP, DEFAULT_SIZE_DP),
                    dotColorArgb = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
                )
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
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

    companion object {
        const val PREFS_NAME = "dot_crosshair_prefs"
        const val KEY_ENABLED = "key_dot_enabled"
        const val KEY_SIZE_DP = "key_dot_size_dp"
        const val KEY_COLOR = "key_dot_color"

        const val DEFAULT_SIZE_DP = 6
        const val MIN_SIZE_DP = 2
        const val MAX_SIZE_DP = 24
        val DEFAULT_COLOR = 0xFF00FF66.toInt() // Neon Green
    }
}
