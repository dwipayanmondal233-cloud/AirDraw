package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.roundToInt

class SamplesActivity : Activity() {

    private var name = ""
    private lateinit var summary: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var info: TextView
    private lateinit var holdBtn: Button
    private lateinit var recorder: GestureRecorder
    private val maxSamples = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        name = intent.getStringExtra("name") ?: ""

        recorder = GestureRecorder(this) { pts, _ -> onRecorded(pts) }

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Samples: " + name, 24f, Ui.CYAN))

        summary = Ui.text(this, "", 14f, Ui.GRAY)
        root.addView(summary, Ui.params(this, Ui.WRAP, 6))

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.addView(listBox)
        root.addView(scroll, Ui.params(this, 0, 10, 1f))

        info = Ui.text(this, "Hold the button and draw a new sample.", 13f, Ui.PURPLE)
        root.addView(info, Ui.params(this, Ui.WRAP, 8))

        holdBtn = Ui.button(this, "HOLD TO ADD A SAMPLE")
        holdBtn.textSize = 18f
        holdBtn.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!recorder.available) {
                        info.text = "No gyroscope on this phone."
                    } else {
                        recorder.start()
                        info.text = "Recording..."
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> recorder.stop()
            }
            true
        }
        root.addView(holdBtn, Ui.params(this, Ui.dp(this, 72), 8))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        listBox.removeAllViews()
        val sym = Store.getSymbol(name)
        if (sym == null) {
            summary.text = "Symbol not found."
            holdBtn.isEnabled = false
            holdBtn.alpha = 0.4f
            return
        }
        val samples = sym.samples
        val n = samples.size

        val avg = DoubleArray(n)
        if (n >= 2) {
            for (i in 0 until n) {
                var sum = 0.0
                var cnt = 0
                for (j in 0 until n) {
                    if (i != j) {
                        sum += Recognizer.similarity(samples[i], samples[j], Store.rotInv)
                        cnt++
                    }
                }
                avg[i] = sum / cnt
            }
        }

        if (n < 2) {
            summary.text = n.toString() + " sample. Add more. 3 or more is best."
        } else {
            val overall = (avg.average() * 100).roundToInt()
            val verdict = when {
                overall >= 85 -> "Great"
                overall >= 70 -> "Good"
                else -> "Samples differ a lot. Delete the worst and record again."
            }
            summary.text = n.toString() + " samples | Consistency " + overall + "% | " + verdict
        }

        for (i in 0 until n) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundColor(Color.parseColor("#14142A"))
            row.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8))

            val thumb = MiniPathView(this)
            thumb.setPath(samples[i])
            row.addView(thumb, LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)))

            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.addView(Ui.text(this, "Sample " + (i + 1), 16f, Ui.CYAN, false))
            if (n >= 2) {
                val pct = (avg[i] * 100).roundToInt()
                val good = pct >= 70
                col.addView(
                    Ui.text(
                        this,
                        (if (good) "\u2713 " else "\u2715 ") + "fits the others: " + pct + "%",
                        13f,
                        if (good) "#00FF88" else "#FF4466",
                        false
                    )
                )
            } else {
                col.addView(Ui.text(this, "Only one sample", 13f, Ui.GRAY, false))
            }
            val lp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            lp.leftMargin = Ui.dp(this, 12)
            row.addView(col, lp)

            val del = Ui.button(this, "Delete", "#AA2244")
            del.setOnClickListener { delete(i) }
            row.addView(del, LinearLayout.LayoutParams(Ui.WRAP, Ui.dp(this, 46)))

            listBox.addView(row, Ui.params(this, Ui.WRAP, 8))
        }

        val full = n >= maxSamples
        holdBtn.isEnabled = !full
        holdBtn.alpha = if (full) 0.4f else 1f
        if (full) info.text = "Maximum " + maxSamples + " samples. Delete one to add another."
    }

    private fun delete(i: Int) {
        val sym = Store.getSymbol(name) ?: return
        if (sym.samples.size <= 1) {
            info.text = "Keep at least 1 sample. To remove the whole symbol, use Delete on the main screen."
            return
        }
        Store.removeSample(name, i)
        info.text = "Sample deleted."
        refresh()
    }

    private fun onRecorded(pts: List<PointF>) {
        if (Recognizer.isTooSmall(pts)) {
            info.text = "Too small. Draw bigger and slower."
            return
        }
        val sample = Recognizer.resampleRaw(pts)
        var msg = "Sample added."
        val sym = Store.getSymbol(name)
        if (sym != null && sym.samples.isNotEmpty()) {
            var best = 0.0
            for (s in sym.samples) {
                best = max(best, Recognizer.similarity(sample, s, Store.rotInv))
            }
            val pct = (best * 100).roundToInt()
            msg = if (pct >= 60) {
                "Added. It matches your others (" + pct + "%)."
            } else {
                "Added, but it looks different (" + pct + "%). Delete it if it was a mistake."
            }
        }
        Store.addSample(name, sample)
        info.text = msg
        refresh()
    }

    override fun onPause() {
        super.onPause()
        recorder.cancel()
    }
}