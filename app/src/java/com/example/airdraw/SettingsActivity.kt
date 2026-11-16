package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Settings", 26f, Ui.CYAN))

        // Threshold
        val thLabel = Ui.text(this, "", 15f, Ui.GRAY, false)
        thLabel.text = "Match threshold: " + Store.threshold + "% (higher = stricter)"
        val thBar = SeekBar(this)
        thBar.max = 65
        thBar.progress = Store.threshold - 30
        thBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                Store.threshold = p + 30
                thLabel.text = "Match threshold: " + (p + 30) + "% (higher = stricter)"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(thLabel, Ui.params(this, Ui.WRAP, 20))
        root.addView(thBar, Ui.params(this, Ui.WRAP, 4))

        // Recording time
        val rtLabel = Ui.text(this, "", 15f, Ui.GRAY, false)
        rtLabel.text = "Recording time: " + (Store.recordMs / 1000.0) + " s"
        val rtBar = SeekBar(this)
        rtBar.max = 20
        rtBar.progress = ((Store.recordMs - 1000) / 100).coerceIn(0, 20)
        rtBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val ms = 1000 + p * 100
                Store.recordMs = ms
                rtLabel.text = "Recording time: " + (ms / 1000.0) + " s"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(rtLabel, Ui.params(this, Ui.WRAP, 20))
        root.addView(rtBar, Ui.params(this, Ui.WRAP, 4))

        // Checkboxes
        val rot = CheckBox(this)
        rot.text = "Rotation invariant (ignores tilt. Z and N may look alike, 6 and 9 too)"
        rot.setTextColor(Color.WHITE)
        rot.isChecked = Store.rotInv
        rot.setOnCheckedChangeListener { _, v -> Store.rotInv = v }
        root.addView(rot, Ui.params(this, Ui.WRAP, 20))

        val fx = CheckBox(this)
        fx.text = "Flip X (mirror left/right)"
        fx.setTextColor(Color.WHITE)
        fx.isChecked = Store.flipX
        fx.setOnCheckedChangeListener { _, v -> Store.flipX = v }
        root.addView(fx, Ui.params(this, Ui.WRAP, 8))

        val fy = CheckBox(this)
        fy.text = "Flip Y (mirror up/down)"
        fy.setTextColor(Color.WHITE)
        fy.isChecked = Store.flipY
        fy.setOnCheckedChangeListener { _, v -> Store.flipY = v }
        root.addView(fy, Ui.params(this, Ui.WRAP, 8))

        root.addView(
            Ui.text(
                this,
                "After changing Flip or Rotation, re-teach your symbols so they match.",
                13f, Ui.PURPLE, false
            ),
            Ui.params(this, Ui.WRAP, 20)
        )

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Ui.BG))
        scroll.addView(root)
        setContentView(scroll)
    }
}