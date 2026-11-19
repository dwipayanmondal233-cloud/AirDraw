package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.widget.CheckBox
import android.widget.TextView

class PracticeActivity : Activity() {

    private lateinit var pathView: PathView
    private lateinit var result: TextView
    private lateinit var detail: TextView
    private lateinit var runBox: CheckBox
    private lateinit var recorder: GestureRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ActionRunner.init(this)

         recorder = GestureRecorder(this) { pts, _ -> onRecorded(pts) }
        recorder.onLive = { pathView.setLive(it) }

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Practice", 26f, Ui.CYAN))
        root.addView(
            Ui.text(this, "Draw and see what AirDraw thinks.", 13f, Ui.PURPLE),
            Ui.params(this, Ui.WRAP, 4)
        )

        pathView = PathView(this)
        root.addView(pathView, Ui.params(this, 0, 8, 1f))

        result = Ui.text(this, "-", 34f, Ui.CYAN)
        root.addView(result, Ui.params(this, Ui.WRAP, 8))

        detail = Ui.text(this, "Hold the button and draw.", 14f, Ui.GRAY)
        root.addView(detail, Ui.params(this, Ui.WRAP, 2))

        runBox = CheckBox(this)
        runBox.text = "Run the action on match"
        runBox.setTextColor(Color.WHITE)
        root.addView(runBox, Ui.params(this, Ui.WRAP, 6))

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
        val v = GestureEngine.judge(pts)
        if (!v.accepted) {
            result.setTextColor(Color.parseColor("#FF4466"))
            result.text = v.title
            detail.text = v.detail
            return
        }
        result.setTextColor(Color.parseColor("#00FF88"))
        result.text = v.title
        var msg = v.detail
        val sym = v.symbol
        if (runBox.isChecked && sym != null) {
            msg = msg + ". " + ActionRunner.run(this, sym.action)
        }
        detail.text = msg
    }

    override fun onPause() {
        super.onPause()
        recorder.cancel()
    }
}