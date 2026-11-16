package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import kotlin.math.roundToInt

class PracticeActivity : Activity() {

    private lateinit var pathView: PathView
    private lateinit var result: TextView
    private lateinit var detail: TextView
    private lateinit var recorder: GestureRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        recorder = GestureRecorder(this) { pts, _ -> onRecorded(pts) }

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Practice", 26f, Ui.CYAN))
        root.addView(
            Ui.text(this, "No action runs here. Draw and see what AirDraw thinks.", 13f, Ui.PURPLE),
            Ui.params(this, Ui.WRAP, 4)
        )

        pathView = PathView(this)
        root.addView(pathView, Ui.params(this, 0, 8, 1f))

        result = Ui.text(this, "-", 34f, Ui.CYAN)
        root.addView(result, Ui.params(this, Ui.WRAP, 8))

        detail = Ui.text(this, "Hold the button and draw.", 14f, Ui.GRAY)
        root.addView(detail, Ui.params(this, Ui.WRAP, 2))

        val hold = Ui.button(this, "HOLD TO DRAW")
        hold.textSize = 20f
        hold.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!recorder.available) {
                        detail.text = "No gyroscope on this phone."
                    } else {
                        recorder.start()
                        detail.text = "Recording..."
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> recorder.stop()
            }
            true
        }
        root.addView(hold, Ui.params(this, Ui.dp(this, 80), 8))

        setContentView(root)
    }

    private fun onRecorded(pts: List<PointF>) {
        pathView.setPath(pts)
        if (Recognizer.isTooSmall(pts)) {
            result.setTextColor(Color.parseColor("#FF4466"))
            result.text = "Too small"
            detail.text = "Draw bigger and slower."
            return
        }
        val symbols = Store.getSymbols()
        val r = Recognizer.recognize(pts, symbols, Store.rotInv)
        if (r == null) {
            result.setTextColor(Color.parseColor("#FF4466"))
            result.text = "No symbols"
            detail.text = "Teach a symbol first."
            return
        }
        val pct = (r.score * 100).roundToInt()
        val need = Store.threshold
        if (pct >= need) {
            result.setTextColor(Color.parseColor("#00FF88"))
            result.text = r.name
            detail.text = "Score " + pct + "% (needs " + need + "%)"
        } else {
            result.setTextColor(Color.parseColor("#FF4466"))
            result.text = "No match"
            detail.text = "Closest: " + r.name + " " + pct + "% (needs " + need + "%)"
        }
    }

    override fun onPause() {
        super.onPause()
        recorder.cancel()
    }
}