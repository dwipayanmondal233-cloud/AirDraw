package com.example.airdraw

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Settings", 26f, Ui.CYAN))

        // Trigger
        root.addView(
            Ui.text(this, "Trigger", 15f, Ui.GRAY, false),
            Ui.params(this, Ui.WRAP, 16)
        )
        val trTypes = listOf("shake", "volume", "both")
        val trNames = listOf(
            "Shake the phone 3 times",
            "Hold Volume Down (needs Accessibility)",
            "Both"
        )
        val trIds = ArrayList<Int>()
        val trGroup = RadioGroup(this)
        for (i in trTypes.indices) {
            val rb = RadioButton(this)
            rb.text = trNames[i]
            rb.setTextColor(Color.WHITE)
            rb.id = View.generateViewId()
            trIds.add(rb.id)
            trGroup.addView(rb)
        }
        trGroup.check(trIds[trTypes.indexOf(Store.trigger).coerceAtLeast(0)])
        trGroup.setOnCheckedChangeListener { _, id ->
            val i = trIds.indexOf(id)
            if (i >= 0) {
                Store.trigger = trTypes[i]
                AirService.refresh(this)
            }
        }
        root.addView(trGroup, Ui.params(this, Ui.WRAP, 4))

        // Shake strength
        val shLabel = Ui.text(this, "", 15f, Ui.GRAY, false)
        shLabel.text = "Shake strength: " + (Store.shakeG / 10.0) + " g (lower = easier)"
        val shBar = SeekBar(this)
        shBar.max = 25
        shBar.progress = (Store.shakeG - 15).coerceIn(0, 25)
        shBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + 15
                Store.shakeG = v
                shLabel.text = "Shake strength: " + (v / 10.0) + " g (lower = easier)"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(shLabel, Ui.params(this, Ui.WRAP, 20))
        root.addView(shBar, Ui.params(this, Ui.WRAP, 4))

        // Threshold
        val thLabel = Ui.text(this, "", 15f, Ui.GRAY, false)
        thLabel.text = "Match threshold: " + Store.threshold + "% (higher = stricter)"
        val thBar = SeekBar(this)
        thBar.max = 65
        thBar.progress = (Store.threshold - 30).coerceIn(0, 65)
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
            Ui.params(this, Ui.WRAP, 16)
        )

        val vibBox = CheckBox(this)
        vibBox.text = "Vibration feedback"
        vibBox.setTextColor(Color.WHITE)
        vibBox.isChecked = Store.vibration
        vibBox.setOnCheckedChangeListener { _, v -> Store.vibration = v }
        root.addView(vibBox, Ui.params(this, Ui.WRAP, 20))

        val sndBox = CheckBox(this)
        sndBox.text = "Sound feedback"
        sndBox.setTextColor(Color.WHITE)
        sndBox.isChecked = Store.sounds
        sndBox.setOnCheckedChangeListener { _, v -> Store.sounds = v }
        root.addView(sndBox, Ui.params(this, Ui.WRAP, 8))

        val tfBtn = Ui.button(this, "Test sound + vibration", "#444466")
        tfBtn.setOnClickListener {
            Feedback.start(this)
            tfBtn.postDelayed({ Feedback.success(this) }, 800)
            tfBtn.postDelayed({ Feedback.error(this) }, 1700)
        }
        root.addView(tfBtn, Ui.params(this, Ui.dp(this, 52), 12))

        // System shortcuts
        val accBtn = Ui.button(this, "Accessibility settings", "#444466")
        accBtn.setOnClickListener {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        root.addView(accBtn, Ui.params(this, Ui.dp(this, 52), 20))

        val ovBtn = Ui.button(this, "Overlay permission (backup for opening apps)", "#444466")
        ovBtn.setOnClickListener {
            open(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
        }
        root.addView(ovBtn, Ui.params(this, Ui.dp(this, 52), 8))

        val batBtn = Ui.button(this, "Battery settings", "#444466")
        batBtn.setOnClickListener {
            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS_SETTINGS))
        }
        root.addView(batBtn, Ui.params(this, Ui.dp(this, 52), 8))

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Ui.BG))
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun open(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}