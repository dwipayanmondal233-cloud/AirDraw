package com.example.airdraw

import kotlin.math.abs
import kotlin.math.max

// Finds a double tap on the BACK of the phone from accelerometer readings.
class TapDetector {

    var threshold = 650f

    var peakBack = 0f
        private set

    private var have = false
    private var px = 0f
    private var py = 0f
    private var pz = 0f
    private var pT = 0L
    private var ema = 0f
    private var firstTapMs = 0L
    private var lastSpikeMs = 0L
    private var betweenMax = 0f

    fun reset() {
        have = false
        ema = 0f
        firstTapMs = 0L
        lastSpikeMs = 0L
        betweenMax = 0f
    }

    fun takePeak(): Float {
        val p = peakBack
        peakBack = 0f
        return p
    }

    // 0 = nothing, 1 = first tap, 2 = double tap
    fun feed(x: Float, y: Float, z: Float, tNs: Long): Int {
        if (!have) {
            px = x
            py = y
            pz = z
            pT = tNs
            have = true
            return 0
        }
        var dt = (tNs - pT) / 1e9f
        if (dt < 0.002f) return 0
        if (dt > 0.05f) dt = 0.05f

        val jx = abs(x - px) / dt
        val jy = abs(y - py) / dt
        val jz = abs(z - pz) / dt
        px = x
        py = y
        pz = z
        pT = tNs

        val side = max(jx, jy)
        if (jz > peakBack) peakBack = jz

        val tMs = tNs / 1_000_000L
        // a back tap pushes along the screen's depth axis (z), not sideways
        val spike = jz >= threshold && jz >= 1.3f * side
        var result = 0

        if (spike) {
            if (tMs - lastSpikeMs > 70) {
                val gap = tMs - firstTapMs
                if (firstTapMs != 0L && gap >= 110L && gap <= 520L &&
                    betweenMax < threshold * 0.6f
                ) {
                    result = 2
                    firstTapMs = 0L
                } else if (ema < threshold * 0.35f) {
                    firstTapMs = tMs
                    betweenMax = 0f
                    result = 1
                } else {
                    firstTapMs = 0L
                }
                lastSpikeMs = tMs
            }
        } else if (firstTapMs != 0L) {
            if (tMs - firstTapMs > 520L) {
                firstTapMs = 0L
            } else if (tMs - lastSpikeMs > 70) {
                betweenMax = max(betweenMax, max(side, jz))
            }
        }

        ema = 0.97f * ema + 0.03f * max(side, jz)
        return result
    }
}