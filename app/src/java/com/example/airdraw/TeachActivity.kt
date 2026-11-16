package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class TeachActivity : Activity() {

    private lateinit var nameBox: EditText
    private lateinit var pathView: PathView
    private lateinit var info: TextView
    private lateinit var holdBtn: Button
    private lateinit var undoBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var recorder: GestureRecorder

    private val samples = ArrayList<List<PointF>>()
    private val needed = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        recorder = GestureRecorder(this) { pts, _ -> onRecorded(pts) }

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Teach a symbol", 26f, Ui.CYAN))
        root.addView(
            Ui.text(this, "Draw the same shape 3 times. Use the same hand posture each time.", 13f, Ui.PURPLE),
            Ui.params(this, Ui.WRAP, 4)
        )

        nameBox = EditText(this)
        nameBox.hint = "Symbol name (for example Z)"
        nameBox.setHintTextColor(Color.parseColor("#666688"))
        nameBox.setTextColor(Color.WHITE)
        nameBox.isSingleLine = true
        val given = intent.getStringExtra("name")
        if (given != null) nameBox.setText(given)
        root.addView(nameBox, Ui.params(this, Ui.WRAP, 8))

        pathView = PathView(this)
        root.addView(pathView, Ui.params(this, 0, 8, 1f))

        info = Ui.text(this, "", 14f, Ui.GRAY)
        root.addView(info, Ui.params(this, Ui.WRAP, 8))

        undoBtn = Ui.button(this, "Undo last", "#444466")
        undoBtn.setOnClickListener {
            if (samples.isNotEmpty()) samples.removeAt(samples.size - 1)
            updateUi()
        }
        saveBtn = Ui.button(this, "Save symbol", "#0A8F6A")
        saveBtn.setOnClickListener { save() }

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val uLp = LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f)
        uLp.rightMargin = Ui.dp(this, 4)
        val sLp = LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f)
        sLp.leftMargin = Ui.dp(this, 4)
        row.addView(undoBtn, uLp)
        row.addView(saveBtn, sLp)
        root.addView(row, Ui.params(this, Ui.WRAP, 8))

        holdBtn = Ui.button(this, "HOLD TO DRAW")
        holdBtn.textSize = 20f
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
        root.addView(holdBtn, Ui.params(this, Ui.dp(this, 80), 8))

        setContentView(root)
        updateUi()
    }

    private fun onRecorded(pts: List<PointF>) {
        pathView.setPath(pts)
        if (Recognizer.isTooSmall(pts)) {
            info.text = "Too small. Draw bigger and slower."
            return
        }
        samples.add(Recognizer.resampleRaw(pts))
        updateUi()
    }

    private fun updateUi() {
        info.text = "Samples: " + samples.size + " of " + needed
        holdBtn.isEnabled = samples.size < needed
        holdBtn.alpha = if (holdBtn.isEnabled) 1f else 0.4f
        undoBtn.isEnabled = samples.isNotEmpty()
        undoBtn.alpha = if (undoBtn.isEnabled) 1f else 0.4f
        saveBtn.isEnabled = samples.size == needed
        saveBtn.alpha = if (saveBtn.isEnabled) 1f else 0.4f
    }

    private fun save() {
        val name = nameBox.text.toString().trim()
        if (name.isEmpty()) {
            info.text = "Type a symbol name first."
            return
        }
        Store.saveSymbol(name, ArrayList(samples))
        finish()
    }

    override fun onPause() {
        super.onPause()
        recorder.cancel()
    }
}