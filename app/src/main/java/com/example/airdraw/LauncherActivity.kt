package com.example.airdraw

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val trace = prefs.getString("trace", null)
        if (trace == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        showReport(trace)
    }

    private fun showReport(trace: String) {
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0A12"))
        root.setPadding(pad, pad * 2, pad, pad)

        val title = TextView(this)
        title.text = "AirDraw crashed last time"
        title.textSize = 22f
        title.setTextColor(Color.parseColor("#FF4466"))
        root.addView(title)

        val hint = TextView(this)
        hint.text = "Tap Copy report, then paste it in the chat."
        hint.textSize = 14f
        hint.setTextColor(Color.parseColor("#AAAACC"))
        root.addView(hint)

        val scroll = ScrollView(this)
        val tv = TextView(this)
        tv.text = trace
        tv.textSize = 11f
        tv.setTextColor(Color.WHITE)
        tv.setTextIsSelectable(true)
        scroll.addView(tv)
        val sp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        sp.topMargin = pad
        sp.bottomMargin = pad
        root.addView(scroll, sp)

        val copy = Button(this)
        copy.text = "Copy report"
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("AirDraw crash", trace))
            copy.text = "Copied!"
        }
        root.addView(copy)

        val open = Button(this)
        open.text = "Clear report and open app"
        open.setOnClickListener {
            getSharedPreferences("crash", Context.MODE_PRIVATE)
                .edit().remove("trace").commit()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        root.addView(open)

        setContentView(root)
    }
}