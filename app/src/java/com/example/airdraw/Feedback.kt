package com.example.airdraw

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Feedback {

    @Suppress("DEPRECATION")
    private fun vibrator(c: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= 31) {
            val vm = c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Suppress("DEPRECATION")
    private fun play(c: Context, effect: VibrationEffect) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            vibrator(c).vibrate(effect, attrs)
        } catch (e: Exception) {
        }
    }

    fun start(c: Context) {
        play(c, VibrationEffect.createOneShot(40, 200))
    }

    fun success(c: Context) {
        play(c, VibrationEffect.createWaveform(longArrayOf(0, 60, 70, 60), -1))
    }

    fun error(c: Context) {
        play(c, VibrationEffect.createOneShot(250, 60))
    }
}