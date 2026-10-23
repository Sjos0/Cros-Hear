package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Ultra-lightweight custom view that renders a solid circular dot.
 * Operates with minimal memory footprint and zero CPU cycles when idle.
 * Supports hardware-accelerated dynamic offset for gyroscope-assisted aiming.
 */
class DotView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotRadiusPx: Float = 10f
    private var offsetXPx: Float = 0f
    private var offsetYPx: Float = 0f

    fun update(radiusPx: Float, colorArgb: Int) {
        var needsRedraw = false

        if (dotRadiusPx != radiusPx) {
            dotRadiusPx = radiusPx
            needsRedraw = true
        }
        if (paint.color != colorArgb) {
            paint.color = colorArgb
            needsRedraw = true
        }

        if (needsRedraw) {
            invalidate()
        }
    }

    fun setOffset(xPx: Float, yPx: Float) {
        if (offsetXPx != xPx || offsetYPx != yPx) {
            offsetXPx = xPx
            offsetYPx = yPx
            postInvalidateOnAnimation()
        }
    }

    fun resetOffset() {
        if (offsetXPx != 0f || offsetYPx != 0f) {
            offsetXPx = 0f
            offsetYPx = 0f
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val cx = (width / 2f) + offsetXPx
        val cy = (height / 2f) + offsetYPx
        canvas.drawCircle(cx, cy, dotRadiusPx, paint)
    }
}
