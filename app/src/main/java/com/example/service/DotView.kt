package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Ultra-lightweight custom view that renders a solid circular dot.
 * Operates with minimal memory footprint and zero CPU cycles when idle.
 */
class DotView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotRadiusPx: Float = 10f

    fun update(radiusPx: Float, colorArgb: Int) {
        var needsRelayout = false
        var needsRedraw = false

        if (dotRadiusPx != radiusPx) {
            dotRadiusPx = radiusPx
            needsRelayout = true
        }
        if (paint.color != colorArgb) {
            paint.color = colorArgb
            needsRedraw = true
        }

        if (needsRelayout) {
            requestLayout()
        }
        if (needsRelayout || needsRedraw) {
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val diameter = (dotRadiusPx * 2f).toInt().coerceAtLeast(1)
        setMeasuredDimension(diameter, diameter)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, dotRadiusPx, paint)
    }
}
