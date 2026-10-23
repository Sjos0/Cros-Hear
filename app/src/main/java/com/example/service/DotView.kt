package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Ultra-lightweight custom view that renders a solid circular dot.
 * Operates with minimal memory footprint and zero CPU cycles when idle.
 * The dot remains strictly centered at all times as the reference crosshair reticle.
 */
class DotView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotRadiusPx: Float = 10f

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

    override fun onDraw(canvas: Canvas) {
        // Dot is permanently and strictly centered on the screen
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, dotRadiusPx, paint)
    }
}
