package com.example.airdraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MiniPathView(context: Context) : View(context) {

    private var pts: List<PointF> = emptyList()
    private val dens = resources.displayMetrics.density

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0E0E22")
        style = Paint.Style.FILL
    }

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66B040FF")
        style = Paint.Style.STROKE
        strokeWidth = 7f * dens
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dens
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF88")
        style = Paint.Style.FILL
    }

    fun setPath(p: List<PointF>) {
        pts = p.toList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            12f * dens, 12f * dens, bg
        )
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

        val pad = 10f * dens
        val areaW = width - 2 * pad
        val areaH = height - 2 * pad
        val range = max(max(maxX - minX, maxY - minY), 0.05f)
        val scale = min(areaW, areaH) / range
        val offX = pad + (areaW - (maxX - minX) * scale) / 2f
        val offY = pad + (areaH - (maxY - minY) * scale) / 2f

        val path = Path()
        var fx = 0f
        var fy = 0f
        for (i in pts.indices) {
            val x = offX + (pts[i].x - minX) * scale
            val y = offY + (pts[i].y - minY) * scale
            if (i == 0) {
                path.moveTo(x, y)
                fx = x
                fy = y
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, glow)
        canvas.drawPath(path, line)
        canvas.drawCircle(fx, fy, 3f * dens, dot)
    }
}