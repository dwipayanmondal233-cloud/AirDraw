package com.example.airdraw

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Random
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class PathView(context: Context) : View(context) {

    private var pts: List<PointF> = emptyList()
    private var progress = 1f
    private var live = false
    private var twinkle = 0f
    private var anim: ValueAnimator? = null
    private var twAnim: ValueAnimator? = null

    // x, y (0..1), radius, phase
    private val stars: List<FloatArray>

    init {
        val rnd = Random(7)
        val list = ArrayList<FloatArray>()
        for (i in 0 until 36) {
            list.add(
                floatArrayOf(
                    rnd.nextFloat(),
                    rnd.nextFloat(),
                    1.5f + rnd.nextFloat() * 2f,
                    rnd.nextFloat() * 6.28f
                )
            )
        }
        stars = list
    }

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66B040FF")
        style = Paint.Style.STROKE
        strokeWidth = 24f
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

    private val headGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6600E5FF")
        style = Paint.Style.FILL
    }

    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4433AA")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Final path: fits the view and draws itself with an animation.
    fun setPath(points: List<PointF>) {
        live = false
        pts = points.toList()
        anim?.cancel()
        if (pts.size < 2) {
            progress = 1f
            invalidate()
            return
        }
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 800
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        a.start()
        anim = a
    }

    // Live trail while you move. The scale stays fixed so it does not jump.
    fun setLive(points: List<PointF>) {
        anim?.cancel()
        live = true
        progress = 1f
        pts = points
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val t = ValueAnimator.ofFloat(0f, (2 * PI).toFloat())
        t.duration = 4000
        t.repeatCount = ValueAnimator.INFINITE
        t.interpolator = LinearInterpolator()
        t.addUpdateListener {
            twinkle = it.animatedValue as Float
            invalidate()
        }
        t.start()
        twAnim = t
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        twAnim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(2f, 2f, width - 2f, height - 2f, 28f, 28f, border)

        for (s in stars) {
            val a = (90 + 90 * sin(twinkle + s[3])).toInt().coerceIn(0, 255)
            starPaint.alpha = a
            canvas.drawCircle(s[0] * width, s[1] * height, s[2], starPaint)
        }

        if (pts.size < 2) return

        val sc: Float
        val ox: Float
        val oy: Float
        if (live) {
            sc = min(width, height) / 1.2f
            ox = width / 2f
            oy = height / 2f
        } else {
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
            sc = min(areaW, areaH) / range
            ox = pad + (areaW - (maxX - minX) * sc) / 2f - minX * sc
            oy = pad + (areaH - (maxY - minY) * sc) / 2f - minY * sc
        }

        val count = max(2, (progress * (pts.size - 1)).toInt() + 1).coerceAtMost(pts.size)

        val path = Path()
        var firstX = 0f
        var firstY = 0f
        var lastX = 0f
        var lastY = 0f
        for (i in 0 until count) {
            val x = ox + pts[i].x * sc
            val y = oy + pts[i].y * sc
            if (i == 0) {
                path.moveTo(x, y)
                firstX = x
                firstY = y
            } else {
                path.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }

        canvas.drawPath(path, glow)
        canvas.drawPath(path, line)
        canvas.drawCircle(firstX, firstY, 14f, startPaint)
        if (live || progress < 1f) {
            canvas.drawCircle(lastX, lastY, 26f, headGlow)
            canvas.drawCircle(lastX, lastY, 10f, headPaint)
        } else {
            canvas.drawCircle(lastX, lastY, 14f, endPaint)
        }
    }
}