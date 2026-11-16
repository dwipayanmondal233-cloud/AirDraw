package com.example.airdraw

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var calText: TextView
    private var fromCal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        root = Ui.root(this)
        root.addView(Ui.text(this, "Settings", 26f, Ui.CYAN))

        // ---------- Trigger ----------
        header("Trigger")
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

        seek(15, 40, 1, Store.shakeG,
            { "Shake strength: " + (it / 10.0) + " g (lower = easier)" }) { Store.shakeG = it }
        seek(200, 1000, 50, Store.shakeDelay,
            { "Pause after the shake buzz: " + (it / 1000.0) + " s" }) { Store.shakeDelay = it }
        seek(0, 6000, 250, Store.cooldownMs,
            { "Cooldown between gestures: " + (it / 1000.0) + " s" }) { Store.cooldownMs = it }
        check("Only run when the screen is on (stops pocket triggers)", Store.screenOnly) {
            Store.screenOnly = it
        }

        // ---------- Recognition ----------
        header("Recognition")
        seek(30, 95, 1, Store.threshold,
            { "Match threshold: " + it + "% (higher = stricter)" }) { Store.threshold = it }
        seek(0, 20, 1, Store.margin,
            { "Ambiguity guard: " + it + "% (gap to the 2nd best symbol)" }) { Store.margin = it }
        seek(0, 25, 1, Store.callExtra,
            { "Extra safety for Call symbols: +" + it + "%" }) { Store.callExtra = it }
        seek(0, 25, 1, Store.tilt,
            { "Tilt tolerance: " + it + " degrees" }) { Store.tilt = it }
        seek(5, 40, 1, Store.minSize,
            { "Smallest gesture: " + (it / 100.0) + " rad" }) { Store.minSize = it }
        seek(1000, 3000, 100, Store.recordMs,
            { "Recording time: " + (it / 1000.0) + " s" }) { Store.recordMs = it }
        check("Rotation invariant (ignores tilt. Z and N may look alike, 6 and 9 too)", Store.rotInv) {
            Store.rotInv = it
        }
        check("Flip X (mirror left/right)", Store.flipX) { Store.flipX = it }
        check("Flip Y (mirror up/down)", Store.flipY) { Store.flipY = it }
        note("After changing Flip, Rotation or Calibration, re-teach your symbols.")

        // ---------- Calibration ----------
        header("Calibration")
        calText = Ui.text(this, calStatus(), 14f, Ui.GRAY, false)
        root.addView(calText, Ui.params(this, Ui.WRAP, 4))
        seek(0, 60, 1, Store.deadZone,
            { "Dead zone: " + (it / 1000.0) + " rad/s (ignores tiny drift)" }) { Store.deadZone = it }
        btn("Calibrate gyroscope", "#7B2FFF") {
            fromCal = true
            startActivity(Intent(this, CalibrateActivity::class.java))
        }

        // ---------- Feedback ----------
        header("Feedback")
        check("Vibration feedback", Store.vibration) { Store.vibration = it }
        check("Sound feedback", Store.sounds) { Store.sounds = it }
        val tf = Ui.button(this, "Test sound + vibration", "#444466")
        tf.setOnClickListener {
            Feedback.start(this)
            tf.postDelayed({ Feedback.success(this) }, 800)
            tf.postDelayed({ Feedback.error(this) }, 1700)
        }
        root.addView(tf, Ui.params(this, Ui.dp(this, 52), 12))

        // ---------- System ----------
        header("System")
        btn("Accessibility settings", "#444466") {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("Overlay permission (backup for opening apps)", "#444466") {
            open(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
        }
        btn("Battery settings", "#444466") {
            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATIONS_SETTINGS))
        }
        btn("Reset all settings to default", "#AA2244") {
            AlertDialog.Builder(this)
                .setTitle("Reset all settings?")
                .setMessage("Your symbols are kept. Only settings and calibration are reset.")
                .setPositiveButton("Reset") { _, _ ->
                    Store.resetSettings()
                    AirService.refresh(this)
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Ui.BG))
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (fromCal) {
            fromCal = false
            recreate()
        }
    }

    private fun calStatus(): String {
        return if (Store.calibrated) "Gyroscope calibrated: yes" else "Gyroscope calibrated: no"
    }

    private fun header(title: String) {
        root.addView(Ui.text(this, title, 18f, Ui.CYAN, false), Ui.params(this, Ui.WRAP, 30))
    }

    private fun note(text: String) {
        root.addView(Ui.text(this, text, 13f, Ui.PURPLE, false), Ui.params(this, Ui.WRAP, 12))
    }

    private fun check(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
        val cb = CheckBox(this)
        cb.text = text
        cb.setTextColor(Color.WHITE)
        cb.isChecked = value
        cb.setOnCheckedChangeListener { _, v -> onChange(v) }
        root.addView(cb, Ui.params(this, Ui.WRAP, 12))
    }

    private fun btn(text: String, color: String, onClick: () -> Unit) {
        val b = Ui.button(this, text, color)
        b.setOnClickListener { onClick() }
        root.addView(b, Ui.params(this, Ui.dp(this, 52), 12))
    }

    private fun seek(
        min: Int, max: Int, step: Int, value: Int,
        label: (Int) -> String, onChange: (Int) -> Unit
    ) {
        val tv = Ui.text(this, label(value), 15f, Ui.GRAY, false)
        val bar = SeekBar(this)
        bar.max = (max - min) / step
        bar.progress = ((value - min) / step).coerceIn(0, (max - min) / step)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = min + p * step
                tv.text = label(v)
                if (fromUser) onChange(v)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(tv, Ui.params(this, Ui.WRAP, 18))
        root.addView(bar, Ui.params(this, Ui.WRAP, 2))
    }

    private fun open(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}