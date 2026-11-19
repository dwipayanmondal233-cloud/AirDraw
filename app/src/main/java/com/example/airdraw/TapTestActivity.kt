package com.example.airdraw

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.SeekBar
import android.widget.TextView

class TapTestActivity : Activity(), SensorEventListener {

    private lateinit var sm: SensorManager
    private var accel: Sensor? = null
    private val tap = TapDetector()

    private lateinit var counts: TextView
    private lateinit var peak: TextView
    private lateinit var flash: TextView
    private lateinit var level: TextView

    private var singles = 0
    private var doubles = 0
    private var lastUi = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Back double-tap test", 24f, Ui.CYAN))
        root.addView(
            Ui.text(
                this,
                "Hold the phone the way you normally do. Tap the BACK twice, quickly, with a finger.\n\n" +
                    "Turn sensitivity up until your double taps count. Turn it down if walking or " +
                    "waving the phone counts by mistake.",
                14f, Ui.GRAY, false
            ),
            Ui.params(this, Ui.WRAP, 12)
        )

        flash = Ui.text(this, "", 20f, "#00FF88")
        root.addView(flash, Ui.params(this, Ui.WRAP, 16))

        counts = Ui.text(this, "", 16f, Ui.CYAN)
        root.addView(counts, Ui.params(this, Ui.WRAP, 8))

        peak = Ui.text(this, "", 14f, Ui.GRAY)
        root.addView(peak, Ui.params(this, Ui.WRAP, 8))

        level = Ui.text(this, "", 15f, Ui.GRAY, false)
        root.addView(level, Ui.params(this, Ui.WRAP, 24))
        val bar = SeekBar(this)
        bar.max = 9
        bar.progress = (Store.tapLevel - 1).coerceIn(0, 9)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                Store.tapLevel = p + 1
                tap.threshold = Store.tapThreshold()
                showLevel()
                if (fromUser) AirService.refresh(this@TapTestActivity)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(bar, Ui.params(this, Ui.WRAP, 2))

        val reset = Ui.button(this, "Reset counters", "#444466")
        reset.setOnClickListener {
            singles = 0
            doubles = 0
            showCounts()
        }
        root.addView(reset, Ui.params(this, Ui.dp(this, 52), 20))

        root.addView(
            Ui.text(
                this,
                "Tip: a phone case or a soft grip softens taps. Tap near the middle of the back.",
                12f, Ui.PURPLE, false
            ),
            Ui.params(this, Ui.WRAP, 16)
        )

        setContentView(root)
        showLevel()
        showCounts()
        peak.text = if (accel == null) "No accelerometer on this phone." else "Waiting for taps..."
    }

    private fun showLevel() {
        level.text = "Back-tap sensitivity: " + Store.tapLevel + " of 10 (higher = easier)"
    }

    private fun showCounts() {
        counts.text = "Single taps: " + singles + "     Double taps: " + doubles
    }

    override fun onResume() {
        super.onResume()
        val a = accel ?: return
        tap.threshold = Store.tapThreshold()
        tap.reset()
        sm.registerListener(this, a, 10000)
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val r = tap.feed(e.values[0], e.values[1], e.values[2], e.timestamp)
        if (r == 1) {
            singles++
            showCounts()
        } else if (r == 2) {
            doubles++
            showCounts()
            flash.text = "\u2713 Double tap!"
            flash.postDelayed({ flash.text = "" }, 900)
            Feedback.start(this)
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastUi > 150) {
            lastUi = now
            val p = tap.takePeak().toInt()
            peak.text = "Strongest back hit just now: " + p +
                "   (needs " + tap.threshold.toInt() + ")"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}