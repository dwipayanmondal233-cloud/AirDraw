package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val time: Long, val ok: Boolean, val text: String)

class HistoryActivity : Activity() {

    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "History", 26f, Ui.CYAN))
        root.addView(
            Ui.text(
                this,
                "The last 40 gestures from your trigger. Green = ran, red = rejected or failed. Use it to find false triggers and near misses.",
                13f, Ui.PURPLE
            ),
            Ui.params(this, Ui.WRAP, 4)
        )

        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.addView(box)
        root.addView(scroll, Ui.params(this, 0, 10, 1f))

        val clear = Ui.button(this, "Clear history", "#AA2244")
        clear.setOnClickListener {
            Store.clearLog()
            refresh()
        }
        root.addView(clear, Ui.params(this, Ui.dp(this, 52), 8))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        box.removeAllViews()
        val items = Store.getLog()
        if (items.isEmpty()) {
            box.addView(
                Ui.text(this, "Nothing yet. Turn AirDraw ON and make a gesture.", 14f, Ui.GRAY),
                Ui.params(this, Ui.WRAP, 20)
            )
            return
        }
        val fmt = SimpleDateFormat("MMM d  HH:mm:ss", Locale.getDefault())
        for (e in items) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.VERTICAL
            row.setBackgroundColor(Color.parseColor("#14142A"))
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8))
            row.addView(Ui.text(this, fmt.format(Date(e.time)), 11f, Ui.GRAY, false))
            row.addView(
                Ui.text(this, e.text, 14f, if (e.ok) "#00FF88" else "#FF4466", false)
            )
            box.addView(row, Ui.params(this, Ui.WRAP, 6))
        }
    }
}