package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A0A12"))
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "AirDraw"
            textSize = 40f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Draw magic in the air"
            textSize = 18f
            setTextColor(Color.parseColor("#B040FF"))
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "Milestone 1: build works!"
            textSize = 14f
            setTextColor(Color.parseColor("#888899"))
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        setContentView(root)
    }
}