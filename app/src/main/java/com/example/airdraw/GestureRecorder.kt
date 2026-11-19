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
import kotlin.math.abs

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
    private var prevWx = 0f
    private var prevWy = 0f
    private var lastTs = 0L
    private var startMs = 0L
    private var lastLiveMs = 0L
    private var flipX = false
    private var flipY = false
    private var bias0 = 0f
    private var bias1 = 0f
    private var dead = 0f

    // called about 30 times a second while recording, to draw the trail live
    var onLive: ((List<PointF>) -> Unit)? = null

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
        prevWx = 0f
        prevWy = 0f
        lastTs = 0L
        lastLiveMs = 0L
        flipX = Store.flipX
        flipY = Store.flipY
        bias0 = if (Store.calibrated) Store.bias0 else 0f
        bias1 = if (Store.calibrated) Store.bias1 else 0f
        dead = Store.deadZone / 1000f
        startMs = SystemClock.elapsedRealtime()
        points.add(PointF(0f, 0f))
        isRecording = true
        // 100 readings per second gives a smoother path
        sm.registerListener(this, g, 10000)
        handler.postDelayed(stopRunnable, Store.recordMs.toLong())
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(stopRunnable)
        sm.unregisterListener(this)
        val ms = SystemClock.elapsedRealtime() - startMs
        onDone(Recognizer.clean(ArrayList(points)), ms)
    }

    fun cancel() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(stopRunnable)
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (!isRecording) return
        var wx = e.values[0] - bias0
        var wy = e.values[1] - bias1
        if (abs(wx) < dead) wx = 0f
        if (abs(wy) < dead) wy = 0f

        if (lastTs != 0L) {
            val dt = (e.timestamp - lastTs) * 1e-9f
            // trapezoid rule: average of the old and new speed
            yaw += 0.5f * (prevWy + wy) * dt
            pitch += 0.5f * (prevWx + wx) * dt
            var x = -yaw
            var y = -pitch
            if (flipX) x = -x
            if (flipY) y = -y
            points.add(PointF(x, y))

            val cb = onLive
            if (cb != null) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastLiveMs >= 33) {
                    lastLiveMs = now
                    cb(ArrayList(points))
                }
            }
        }
        lastTs = e.timestamp
        prevWx = wx
        prevWy = wy
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}