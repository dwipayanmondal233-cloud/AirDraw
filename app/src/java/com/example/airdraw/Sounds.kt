package com.example.airdraw

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

object Sounds {
    private const val RATE = 22050
    private val handler = Handler(Looper.getMainLooper())

    private fun build(notes: List<Pair<Double, Int>>, volume: Double): ShortArray {
        var total = 0
        for (n in notes) total += RATE * n.second / 1000
        val out = ShortArray(total)
        var pos = 0
        for (n in notes) {
            val len = RATE * n.second / 1000
            val fade = minOf(len / 4, RATE * 8 / 1000)
            for (i in 0 until len) {
                var env = 1.0
                if (i < fade) env = i.toDouble() / fade
                if (len - i < fade) env = (len - i).toDouble() / fade
                val v = sin(2.0 * PI * n.first * i / RATE) * env * volume
                out[pos + i] = (v * 32767).toInt().toShort()
            }
            pos += len
        }
        return out
    }

    private val startTone by lazy {
        build(listOf(Pair(880.0, 60)), 0.35)
    }
    private val successTone by lazy {
        build(listOf(Pair(660.0, 90), Pair(880.0, 90), Pair(1320.0, 140)), 0.4)
    }
    private val errorTone by lazy {
        build(listOf(Pair(220.0, 160), Pair(170.0, 200)), 0.35)
    }

    private fun play(samples: ShortArray) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fmt = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(fmt)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            val ms = samples.size * 1000L / RATE
            handler.postDelayed({
                try {
                    track.release()
                } catch (e: Exception) {
                }
            }, ms + 300)
        } catch (e: Exception) {
        }
    }

    fun start() = play(startTone)

    fun success() = play(successTone)

    fun error() = play(errorTone)
}