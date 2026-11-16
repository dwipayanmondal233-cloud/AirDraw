package com.example.airdraw

import android.content.Context
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class GestureRecorder(
    context: Context,
    private val onDone: (List<PointF>, Long) -> Unit
) : SensorEventListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyro: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val handler = Handler(Looper.getMainLooper())
    private val points = ArrayList<PointF>()
    private val stopRunnable = Runnable { stop() }

    private var yaw = 0f
    private var pitch = 0f
    private var lastTs = 0L
    private var startMs = 0L
    private var flipX = false
    private var flipY = false

    var isRecording = false
        private set

    val available: Boolean
        get() = gyro != null

    fun start() {
        val g = gyro ?: return
        if (isRecording) return
        points.clear()
        yaw = 0f
        pitch = 0f
        lastTs = 0L
        flipX = Store.flipX
        flipY = Store.flipY
        startMs = SystemClock.elapsedRealtime()
        points.add(PointF(0f, 0f))
        isRecording = true
        sm.registerListener(this, g, SensorManager.SENSOR_DELAY_GAME)
        handler.postDelayed(stopRunnable, Store.recordMs.toLong())
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(stopRunnable)
        sm.unregisterListener(this)
        val ms = SystemClock.elapsedRealtime() - startMs
        onDone(ArrayList(points), ms)
    }

    fun cancel() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(stopRunnable)
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (!isRecording) return
        if (lastTs != 0L) {
            val dt = (e.timestamp - lastTs) * 1e-9f
            yaw += e.values[1] * dt
            pitch += e.values[0] * dt
            var x = -yaw
            var y = -pitch
            if (flipX) x = -x
            if (flipY) y = -y
            points.add(PointF(x, y))
        }
        lastTs = e.timestamp
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}