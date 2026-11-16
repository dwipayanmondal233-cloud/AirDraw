package com.example.airdraw

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object Perms {

    fun gyro(c: Context): Boolean {
        val sm = c.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun notifications(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun call(c: Context): Boolean {
        return c.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun overlay(c: Context): Boolean = Settings.canDrawOverlays(c)

    fun battery(c: Context): Boolean {
        val pm = c.getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(c.packageName)
    }

    fun flash(c: Context): Boolean {
        return c.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun summary(c: Context): String {
        val list = listOf(
            gyro(c), notifications(c), AirService.running, KeyService.connected,
            call(c), overlay(c), battery(c), flash(c)
        )
        return list.count { it }.toString() + " of " + list.size
    }
}