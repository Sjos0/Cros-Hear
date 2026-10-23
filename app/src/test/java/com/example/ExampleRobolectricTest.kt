package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CrosshairPreferences
import com.example.service.DotView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun testDotViewCreationAndUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dotView = DotView(context)
        assertNotNull(dotView)

        dotView.update(radiusPx = 15f, colorArgb = 0xFF00FF66.toInt())
        assertNotNull(dotView)
    }

    @Test
    fun testPreferencesSaveAndReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs1 = CrosshairPreferences(context)
        prefs1.setSizeDp(14)
        prefs1.setColor(0xFF00E5FF.toInt())
        prefs1.setEnabled(true)
        prefs1.setGyroEnabled(true)
        prefs1.setGyroSensitivity(2.2f)
        prefs1.setGyroStepPx(55)

        val prefs2 = CrosshairPreferences(context)
        assertEquals(14, prefs2.state.value.dotSizeDp)
        assertEquals(0xFF00E5FF.toInt(), prefs2.state.value.dotColorArgb)
        assertTrue(prefs2.state.value.isEnabled)
        assertTrue(prefs2.state.value.isGyroEnabled)
        assertEquals(2.2f, prefs2.state.value.gyroSensitivity, 0.001f)
        assertEquals(55, prefs2.state.value.gyroStepPx)
    }
}
