package com.example.airdraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.View
import kotlin.math.max
import kotlin.math.min

class PathView(context: Context) : View(context) {

    private var pts: List<PointF> = emptyList()

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55B040FF")
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val startPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF88")
        style = Paint.Style.FILL
    }

    private val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4466")
        style = Paint.Style.FILL
    }

    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333355")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun setPath(points: List<PointF>) {
        pts = points.toList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(2f, 2f, width - 2f, height - 2f, border)
        if (pts.size < 2) return

        var minX = pts[0].x
        var maxX = pts[0].x
        var minY = pts[0].y
        var maxY = pts[0].y
        for (p in pts) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }

        val pad = 50f
        val areaW = width - 2 * pad
        val areaH = height - 2 * pad
        val range = max(max(maxX - minX, maxY - minY), 0.1f)
        val scale = min(areaW, areaH) / range
        val offX = pad + (areaW - (maxX - minX) * scale) / 2f
        val offY = pad + (areaH - (maxY - minY) * scale) / 2f

        val path = Path()
        var first = true
        var lastX = 0f
        var lastY = 0f
        var firstX = 0f
        var firstY = 0f
        for (p in pts) {
            val x = offX + (p.x - minX) * scale
            val y = offY + (p.y - minY) * scale
            if (first) {
                path.moveTo(x, y)
                firstX = x
                firstY = y
                first = false
            } else {
                path.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }

        canvas.drawPath(path, glow)
        canvas.drawPath(path, line)
        canvas.drawCircle(firstX, firstY, 14f, startPaint)
        canvas.drawCircle(lastX, lastY, 14f, endPaint)
    }
}