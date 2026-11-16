package com.example.airdraw

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Pt(val x: Double, val y: Double)

data class MatchResult(
    val name: String,
    val score: Double,
    val runnerUp: String = "",
    val runnerUpScore: Double = 0.0
)

object Recognizer {
    const val N = 64
    private const val SIZE = 250.0
    private val HALF_DIAG = 0.5 * sqrt(2.0 * SIZE * SIZE)
    private val ROT_RANGE = Math.toRadians(45.0)
    private val ANGLE_PRECISION = Math.toRadians(2.0)
    private val PHI = 0.5 * (-1.0 + sqrt(5.0))

    fun isTooSmall(raw: List<PointF>): Boolean {
        if (raw.size < 5) return true
        var minX = raw[0].x
        var maxX = raw[0].x
        var minY = raw[0].y
        var maxY = raw[0].y
        for (p in raw) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        return max(maxX - minX, maxY - minY) < Store.minSize / 100f
    }

    // Smooths jitter and cuts the still part at the start and the end.
    fun clean(raw: List<PointF>): List<PointF> {
        if (raw.size < 8) return raw
        val r = (raw.size / 40).coerceIn(1, 3)
        val sm = ArrayList<PointF>()
        for (i in raw.indices) {
            var sx = 0f
            var sy = 0f
            var c = 0
            for (k in -r..r) {
                val j = i + k
                if (j >= 0 && j < raw.size) {
                    sx += raw[j].x
                    sy += raw[j].y
                    c++
                }
            }
            sm.add(PointF(sx / c, sy / c))
        }
        val eps = 0.015f
        var s = 0
        while (s < sm.size - 1 &&
            hypot(sm[s].x - sm[0].x, sm[s].y - sm[0].y) < eps
        ) s++
        var e = sm.size - 1
        val last = sm[sm.size - 1]
        while (e > 0 && hypot(sm[e].x - last.x, sm[e].y - last.y) < eps) e--
        s = max(0, s - 1)
        e = min(sm.size - 1, e + 1)
        if (e - s < 4) return sm
        return ArrayList(sm.subList(s, e + 1))
    }

    fun resampleRaw(raw: List<PointF>): List<PointF> {
        return resample(toPts(raw), N).map { PointF(it.x.toFloat(), it.y.toFloat()) }
    }

    fun recognize(raw: List<PointF>, symbols: List<Symbol>, rotInv: Boolean): MatchResult? {
        if (symbols.isEmpty()) return null
        val cand = normalize(raw, rotInv)
        val range = if (rotInv) ROT_RANGE else Math.toRadians(Store.tilt.toDouble())

        var bestName = ""
        var best = Double.MAX_VALUE
        var secondName = ""
        var second = Double.MAX_VALUE

        for (s in symbols) {
            var symBest = Double.MAX_VALUE
            for (sample in s.samples) {
                val t = normalize(sample, rotInv)
                val d = if (range > 0.0) bestDistance(cand, t, range) else pathDistance(cand, t)
                if (d < symBest) symBest = d
            }
            if (symBest == Double.MAX_VALUE) continue
            if (symBest < best) {
                second = best
                secondName = bestName
                best = symBest
                bestName = s.name
            } else if (symBest < second) {
                second = symBest
                secondName = s.name
            }
        }
        if (bestName.isEmpty()) return null
        val score = max(0.0, 1.0 - best / HALF_DIAG)
        val score2 = if (secondName.isEmpty()) 0.0 else max(0.0, 1.0 - second / HALF_DIAG)
        return MatchResult(bestName, score, secondName, score2)
    }

    private fun toPts(raw: List<PointF>): List<Pt> {
        return raw.map { Pt(it.x.toDouble(), it.y.toDouble()) }
    }

    private fun dist(a: Pt, b: Pt): Double = hypot(a.x - b.x, a.y - b.y)

    private fun pathLength(p: List<Pt>): Double {
        var d = 0.0
        for (i in 1 until p.size) d += dist(p[i - 1], p[i])
        return d
    }

    private fun resample(points: List<Pt>, n: Int): List<Pt> {
        val interval = pathLength(points) / (n - 1)
        if (interval <= 0.0) return List(n) { points[0] }
        val src = ArrayList<Pt>(points)
        val out = ArrayList<Pt>()
        out.add(src[0])
        var acc = 0.0
        var i = 1
        while (i < src.size) {
            val d = dist(src[i - 1], src[i])
            if (d > 0.0 && acc + d >= interval) {
                val t = (interval - acc) / d
                val q = Pt(
                    src[i - 1].x + t * (src[i].x - src[i - 1].x),
                    src[i - 1].y + t * (src[i].y - src[i - 1].y)
                )
                out.add(q)
                src.add(i, q)
                acc = 0.0
            } else {
                acc += d
            }
            i++
        }
        while (out.size < n) out.add(src[src.size - 1])
        while (out.size > n) out.removeAt(out.size - 1)
        return out
    }

    private fun centroid(p: List<Pt>): Pt {
        var x = 0.0
        var y = 0.0
        for (q in p) {
            x += q.x
            y += q.y
        }
        return Pt(x / p.size, y / p.size)
    }

    private fun rotateBy(p: List<Pt>, a: Double): List<Pt> {
        val c = centroid(p)
        val cs = cos(a)
        val sn = sin(a)
        return p.map {
            Pt(
                (it.x - c.x) * cs - (it.y - c.y) * sn + c.x,
                (it.x - c.x) * sn + (it.y - c.y) * cs + c.y
            )
        }
    }

    private fun scaleAndCenter(p: List<Pt>): List<Pt> {
        var minX = p[0].x
        var maxX = p[0].x
        var minY = p[0].y
        var maxY = p[0].y
        for (q in p) {
            minX = min(minX, q.x)
            maxX = max(maxX, q.x)
            minY = min(minY, q.y)
            maxY = max(maxY, q.y)
        }
        val m = max(maxX - minX, maxY - minY)
        val k = if (m > 0.0) SIZE / m else 1.0
        val scaled = p.map { Pt(it.x * k, it.y * k) }
        val c = centroid(scaled)
        return scaled.map { Pt(it.x - c.x, it.y - c.y) }
    }

    private fun normalize(raw: List<PointF>, rotInv: Boolean): List<Pt> {
        var p = resample(toPts(raw), N)
        if (rotInv) {
            val c = centroid(p)
            val a = atan2(c.y - p[0].y, c.x - p[0].x)
            p = rotateBy(p, -a)
        }
        return scaleAndCenter(p)
    }

    private fun pathDistance(a: List<Pt>, b: List<Pt>): Double {
        var d = 0.0
        for (i in a.indices) d += dist(a[i], b[i])
        return d / a.size
    }

    private fun distAt(a: List<Pt>, b: List<Pt>, theta: Double): Double {
        return pathDistance(rotateBy(a, theta), b)
    }

    private fun bestDistance(a: List<Pt>, b: List<Pt>, range: Double): Double {
        var lo = -range
        var hi = range
        var x1 = PHI * lo + (1.0 - PHI) * hi
        var f1 = distAt(a, b, x1)
        var x2 = (1.0 - PHI) * lo + PHI * hi
        var f2 = distAt(a, b, x2)
        while (abs(hi - lo) > ANGLE_PRECISION) {
            if (f1 < f2) {
                hi = x2
                x2 = x1
                f2 = f1
                x1 = PHI * lo + (1.0 - PHI) * hi
                f1 = distAt(a, b, x1)
            } else {
                lo = x1
                x1 = x2
                f1 = f2
                x2 = (1.0 - PHI) * lo + PHI * hi
                f2 = distAt(a, b, x2)
            }
        }
        return min(f1, f2)
    }
}