package com.example.airdraw

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import kotlin.math.sqrt

class AirService : Service(), SensorEventListener {

    companion object {
        @Volatile
        var running = false

        @Volatile
        var instance: AirService? = null

        private const val CHANNEL = "airdraw_service"
        private const val NOTIF_ID = 1

        fun start(c: Context) {
            c.startForegroundService(Intent(c, AirService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, AirService::class.java))
        }

        fun refresh(c: Context) {
            if (running) start(c)
        }
    }

    private lateinit var sm: SensorManager
    private var accel: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var recorder: GestureRecorder

    private var busy = false
    private var volumeMode = false
    private var recStartedAt = 0L

    private var spikes = 0
    private var firstSpike = 0L
    private var lastSpike = 0L

    private val startRec = Runnable {
        if (!recorder.available) {
            busy = false
            updateNote("No gyroscope on this phone.")
        } else {
            recStartedAt = SystemClock.elapsedRealtime()
            recorder.start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        ActionRunner.init(this)
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        recorder = GestureRecorder(this) { pts, _ -> finishGesture(pts) }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "AirDraw service", NotificationManager.IMPORTANCE_LOW)
        )
        val n = buildNotification("Ready. Use your trigger.")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        running = true
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applyTrigger()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recorder.cancel()
        handler.removeCallbacksAndMessages(null)
        sm.unregisterListener(this)
        running = false
        instance = null
        super.onDestroy()
    }

    private fun applyTrigger() {
        sm.unregisterListener(this)
        val t = Store.trigger
        val a = accel
        if ((t == "shake" || t == "both") && a != null) {
            sm.registerListener(this, a, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun volumeEnabled(): Boolean {
        val t = Store.trigger
        return t == "volume" || t == "both"
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wand)
            .setContentTitle("AirDraw is on")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNote(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun finishGesture(pts: List<PointF>) {
        val msg = GestureEngine.process(this, pts)
        busy = false
        updateNote(msg)
    }

    // ---- Shake trigger ----
    override fun onSensorChanged(e: SensorEvent) {
        if (busy) return
        val g = sqrt(
            e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]
        ) / SensorManager.GRAVITY_EARTH
        if (g < Store.shakeG / 10f) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastSpike < 120) return
        lastSpike = now
        if (now - firstSpike > 900) {
            firstSpike = now
            spikes = 0
        }
        spikes++
        if (spikes >= 3) {
            spikes = 0
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onShake() {
        if (busy) return
        busy = true
        Feedback.start(this)
        handler.postDelayed(startRec, 450)
    }

    // ---- Volume trigger (called by KeyService) ----
    fun volumeDown() {
        if (busy) return
        busy = true
        volumeMode = true
        Feedback.start(this)
        handler.postDelayed(startRec, 250)
    }

    fun volumeUp() {
        if (!volumeMode) return
        volumeMode = false
        handler.removeCallbacks(startRec)
        if (recorder.isRecording) {
            if (SystemClock.elapsedRealtime() - recStartedAt < 400) {
                recorder.cancel()
                busy = false
            } else {
                recorder.stop()
            }
        } else {
            busy = false
        }
    }
}