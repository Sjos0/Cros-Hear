package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CrosshairPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    private lateinit var preferences: CrosshairPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = CrosshairPreferences(context)
    }

    @Test
    fun testDefaultState() {
        val state = preferences.state.value
        assertFalse(state.isEnabled)
        assertEquals(CrosshairPreferences.DEFAULT_SIZE_DP, state.dotSizeDp)
        assertEquals(CrosshairPreferences.DEFAULT_COLOR, state.dotColorArgb)
        assertFalse(state.isGyroEnabled)
        assertEquals(CrosshairPreferences.DEFAULT_GYRO_SENSITIVITY, state.gyroSensitivity, 0.001f)
    }

    @Test
    fun testToggleEnabled() {
        preferences.setEnabled(true)
        assertTrue(preferences.state.value.isEnabled)

        preferences.setEnabled(false)
        assertFalse(preferences.state.value.isEnabled)
    }

    @Test
    fun testSizeClamping() {
        preferences.setSizeDp(12)
        assertEquals(12, preferences.state.value.dotSizeDp)

        // Below minimum
        preferences.setSizeDp(0)
        assertEquals(CrosshairPreferences.MIN_SIZE_DP, preferences.state.value.dotSizeDp)

        // Above maximum
        preferences.setSizeDp(50)
        assertEquals(CrosshairPreferences.MAX_SIZE_DP, preferences.state.value.dotSizeDp)
    }

    @Test
    fun testColorUpdate() {
        val testColor = 0xFFFF1744.toInt()
        preferences.setColor(testColor)
        assertEquals(testColor, preferences.state.value.dotColorArgb)
    }

    @Test
    fun testGyroSettings() {
        preferences.setGyroEnabled(true)
        assertTrue(preferences.state.value.isGyroEnabled)

        preferences.setGyroSensitivity(2.5f)
        assertEquals(2.5f, preferences.state.value.gyroSensitivity, 0.001f)

        preferences.setGyroStepPx(60)
        assertEquals(60, preferences.state.value.gyroStepPx)

        preferences.setGyroIntervalMs(90L)
        assertEquals(90L, preferences.state.value.gyroIntervalMs)

        preferences.setGyroInvertX(true)
        assertTrue(preferences.state.value.gyroInvertX)

        preferences.setGyroInvertY(true)
        assertTrue(preferences.state.value.gyroInvertY)

        preferences.calibrateNeutral(0.25f, -0.15f)
        assertEquals(0.25f, preferences.state.value.neutralPitch, 0.001f)
        assertEquals(-0.15f, preferences.state.value.neutralRoll, 0.001f)
    }
}
