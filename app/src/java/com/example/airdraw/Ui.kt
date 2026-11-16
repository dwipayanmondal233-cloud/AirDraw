package com.example.airdraw

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    const val BG = "#0A0A12"
    const val CYAN = "#00E5FF"
    const val PURPLE = "#B040FF"
    const val BTN = "#7B2FFF"
    const val GRAY = "#AAAACC"
    const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(c: Context, n: Int): Int = (n * c.resources.displayMetrics.density).toInt()

    fun root(c: Context): LinearLayout {
        val r = LinearLayout(c)
        r.orientation = LinearLayout.VERTICAL
        r.setBackgroundColor(Color.parseColor(BG))
        r.setPadding(dp(c, 16), dp(c, 24), dp(c, 16), dp(c, 16))
        return r
    }

    fun text(c: Context, s: String, size: Float, color: String, center: Boolean = true): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(Color.parseColor(color))
        if (center) t.gravity = Gravity.CENTER
        if (size >= 22f) {
            t.setShadowLayer(16f, 0f, 0f, Color.parseColor(color))
        }
        return t
    }

    private fun lighten(col: Int): Int {
        val r = Color.red(col) + (255 - Color.red(col)) * 35 / 100
        val g = Color.green(col) + (255 - Color.green(col)) * 35 / 100
        val b = Color.blue(col) + (255 - Color.blue(col)) * 35 / 100
        return Color.rgb(r, g, b)
    }

    fun button(c: Context, s: String, color: String = BTN): Button {
        val base = Color.parseColor(color)
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = dp(c, 14).toFloat()
        shape.setColor(base)
        shape.setStroke(dp(c, 2), lighten(base))
        val ripple = RippleDrawable(
            ColorStateList.valueOf(Color.argb(90, 255, 255, 255)),
            shape,
            null
        )
        val b = Button(c)
        b.text = s
        b.isAllCaps = false
        b.setTextColor(Color.WHITE)
        b.background = ripple
        b.stateListAnimator = null
        return b
    }

    fun params(c: Context, height: Int, topDp: Int = 0, weight: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, weight)
        p.topMargin = dp(c, topDp)
        return p
    }
}