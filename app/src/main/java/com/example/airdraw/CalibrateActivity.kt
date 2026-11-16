package com.example.airdraw

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView

class CalibrateActivity : Activity(), SensorEventListener {

    private lateinit var sm: SensorManager
    private var gyro: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var info: TextView
    private lateinit var startBtn: Button

    private var running = false
    private var n = 0
    private val sum = DoubleArray(3)
    private val sumSq = DoubleArray(3)
    private val finishRun = Runnable { finishCalibration() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Calibrate gyroscope", 24f, Ui.CYAN))
        root.addView(
            Ui.text(
                this,
                "Every gyroscope has a tiny error, so a still phone looks like it is slowly turning. " +
                    "This measures that error and removes it.\n\n" +
                    "1. Put the phone flat on a table.\n" +
                    "2. Do not touch it or the table.\n" +
                    "3. Tap Start and wait 3 seconds.",
                14f, Ui.GRAY, false
            ),
            Ui.params(this, Ui.WRAP, 16)
        )

        info = Ui.text(this, status(), 15f, Ui.PURPLE, false)
        root.addView(info, Ui.params(this, Ui.WRAP, 20))

        startBtn = Ui.button(this, "Start (3 seconds)")
        startBtn.setOnClickListener { startCalibration() }
        root.addView(startBtn, Ui.params(this, Ui.dp(this, 60), 20))

        val reset = Ui.button(this, "Reset calibration", "#444466")
        reset.setOnClickListener {
            Store.bias0 = 0f
            Store.bias1 = 0f
            Store.calibrated = false
            info.text = status()
        }
        root.addView(reset, Ui.params(this, Ui.dp(this, 52), 10))

        setContentView(root)
    }

    private fun r4(x: Double): String = (Math.round(x * 10000) / 10000.0).toString()

    private fun status(): String {
        return if (Store.calibrated) {
            "Calibrated: yes (bias " + r4(Store.bias0.toDouble()) + ", " +
                r4(Store.bias1.toDouble()) + " rad/s)"
        } else {
            "Calibrated: no"
        }
    }

    private fun startCalibration() {
        val g = gyro
        if (g == null) {
            info.text = "No gyroscope on this phone."
            return
        }
        if (running) return
        n = 0
        sum.fill(0.0)
        sumSq.fill(0.0)
        running = true
        startBtn.isEnabled = false
        startBtn.alpha = 0.4f
        info.text = "Measuring... keep the phone still."
        sm.registerListener(this, g, SensorManager.SENSOR_DELAY_GAME)
        handler.postDelayed(finishRun, 3000)
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (!running) return
        for (i in 0..2) {
            val v = e.values[i].toDouble()
            sum[i] += v
            sumSq[i] += v * v
        }
        n++
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun finishCalibration() {
        running = false
        sm.unregisterListener(this)
        startBtn.isEnabled = true
        startBtn.alpha = 1f

        if (n < 30) {
            info.text = "Not enough data. Try again."
            return
        }
        val mean = DoubleArray(3) { sum[it] / n }
        var noise = 0.0
        var drift = 0.0
        for (i in 0..2) {
            val variance = sumSq[i] / n - mean[i] * mean[i]
            val sd = Math.sqrt(Math.max(0.0, variance))
            if (sd > noise) noise = sd
            if (Math.abs(mean[i]) > drift) drift = Math.abs(mean[i])
        }
        if (noise > 0.02 || drift > 0.15) {
            info.text = "The phone moved (noise " + r4(noise) +
                "). Put it flat on a table and try again."
            return
        }
        Store.bias0 = mean[0].toFloat()
        Store.bias1 = mean[1].toFloat()
        Store.calibrated = true
        val dz = (noise * 3000).toInt().coerceIn(5, 60)
        Store.deadZone = dz
        info.text = "Calibrated \u2713\nBias: " + r4(mean[0]) + ", " + r4(mean[1]) +
            " rad/s\nNoise: " + r4(noise) + "\nDead zone set to " + (dz / 1000.0) +
            "\n\nRe-teach your symbols for best accuracy."
    }

    override fun onPause() {
        super.onPause()
        if (running) {
            running = false
            handler.removeCallbacks(finishRun)
            sm.unregisterListener(this)
            startBtn.isEnabled = true
            startBtn.alpha = 1f
            info.text = status()
        }
    }
}