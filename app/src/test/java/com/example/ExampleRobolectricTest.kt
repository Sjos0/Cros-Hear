package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CrosshairPreferences
import com.example.service.DotView
import org.junit.Assert.assertNotNull
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

        val prefs2 = CrosshairPreferences(context)
        org.junit.Assert.assertEquals(14, prefs2.state.value.dotSizeDp)
        org.junit.Assert.assertEquals(0xFF00E5FF.toInt(), prefs2.state.value.dotColorArgb)
        org.junit.Assert.assertTrue(prefs2.state.value.isEnabled)
    }
}
