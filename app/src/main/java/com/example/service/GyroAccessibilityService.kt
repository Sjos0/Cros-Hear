package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that simulates touch gestures (clicar e arrastar na tela)
 * guided by the gyroscope orientation in 8 discrete directions.
 */
class GyroAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event handling required, this service is dedicated to gesture dispatch
    }

    override fun onInterrupt() {
        // Interruption callback
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 35
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        var instance: GyroAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun dispatchSwipe(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            durationMs: Long = 35
        ): Boolean {
            return instance?.performSwipe(startX, startY, endX, endY, durationMs) ?: false
        }
    }
}
