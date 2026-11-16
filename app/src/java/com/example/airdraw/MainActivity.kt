package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyro: Sensor? = null

    private lateinit var pathView: PathView
    private lateinit var info: TextView
    private lateinit var flipX: CheckBox
    private lateinit var flipY: CheckBox

    private val handler = Handler(Looper.getMainLooper())
    private val points = ArrayList<PointF>()

    private var recording = false
    private var yaw = 0f
    private var pitch = 0f
    private var lastTs = 0L
    private var startMs = 0L

    private val maxMs = 1500L
    private val stopRunnable = Runnable { stopRecording() }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A12"))
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "AirDraw"
            textSize = 30f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
        }

        val hint = TextView(this).apply {
            text = "Hold the phone upright, screen facing you. Press and hold the button, draw a big shape in the air, then let go."
            textSize = 13f
            setTextColor(Color.parseColor("#B040FF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(10))
        }

        pathView = PathView(this)

        info = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#AAAACC"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
            text = if (gyro == null) "No gyroscope found on this phone." else "Ready."
        }

        flipX = CheckBox(this).apply {
            text = "Flip X"
            setTextColor(Color.WHITE)
        }
        flipY = CheckBox(this).apply {
            text = "Flip Y"
            setTextColor(Color.WHITE)
        }

        val flipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        flipRow.addView(flipX)
        flipRow.addView(flipY)

        val btn = Button(this).apply {
            text = "HOLD TO DRAW"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#7B2FFF"))
        }
        btn.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopRecording()
            }
            true
        }

        root.addView(title)
        root.addView(hint)
        root.addView(
            pathView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        root.addView(info)
        root.addView(flipRow)
        root.addView(
            btn,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(80))
        )
        setContentView(root)
    }

    private fun startRecording() {
        val g = gyro ?: return
        if (recording) return
        points.clear()
        yaw = 0f
        pitch = 0f
        lastTs = 0L
        startMs = SystemClock.elapsedRealtime()
        points.add(PointF(0f, 0f))
        recording = true
        sensorManager.registerListener(this, g, SensorManager.SENSOR_DELAY_GAME)
        handler.postDelayed(stopRunnable, maxMs)
        info.text = "Recording..."
    }

    private fun stopRecording() {
        if (!recording) return
        recording = false
        handler.removeCallbacks(stopRunnable)
        sensorManager.unregisterListener(this)
        val ms = SystemClock.elapsedRealtime() - startMs
        pathView.setPath(points)
        info.text = "Done: " + points.size + " points, " + ms + " ms"
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (!recording) return
        if (lastTs != 0L) {
            val dt = (e.timestamp - lastTs) * 1e-9f
            val gx = e.values[0]
            val gy = e.values[1]
            yaw += gy * dt
            pitch += gx * dt
            var x = -yaw
            var y = -pitch
            if (flipX.isChecked) x = -x
            if (flipY.isChecked) y = -y
            points.add(PointF(x, y))
        }
        lastTs = e.timestamp
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        stopRecording()
    }
}