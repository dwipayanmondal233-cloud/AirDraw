package com.example.airdraw

import android.content.Context
import android.graphics.PointF
import kotlin.math.roundToInt

data class Verdict(
    val accepted: Boolean,
    val symbol: Symbol?,
    val pct: Int,
    val title: String,
    val detail: String
)

object GestureEngine {

    fun judge(pts: List<PointF>): Verdict {
        if (Recognizer.isTooSmall(pts)) {
            return Verdict(false, null, 0, "Too small", "Draw bigger and slower.")
        }
        val symbols = Store.getSymbols()
        val r = Recognizer.recognize(pts, symbols, Store.rotInv)
        if (r == null) {
            return Verdict(false, null, 0, "No symbols", "Teach a symbol first.")
        }
        val sym = symbols.firstOrNull { it.name == r.name }
        if (sym == null) {
            return Verdict(false, null, 0, "No symbols", "Teach a symbol first.")
        }
        val pct = (r.score * 100).roundToInt()
        val need = Store.threshold + (if (sym.action.type == "call") Store.callExtra else 0)
        if (pct < need) {
            return Verdict(
                false, sym, pct, "No match",
                "Closest: " + r.name + " " + pct + "% (needs " + need + "%)"
            )
        }
        if (r.runnerUp.isNotEmpty()) {
            val p2 = (r.runnerUpScore * 100).roundToInt()
            if (pct - p2 < Store.margin) {
                return Verdict(
                    false, sym, pct, "Unsure",
                    r.name + " " + pct + "% vs " + r.runnerUp + " " + p2 + "% (too close)"
                )
            }
            return Verdict(
                true, sym, pct, r.name,
                "Score " + pct + "% (needs " + need + "%), next best " + r.runnerUp + " " + p2 + "%"
            )
        }
        return Verdict(true, sym, pct, r.name, "Score " + pct + "% (needs " + need + "%)")
    }

    fun process(c: Context, pts: List<PointF>): String {
        val v = judge(pts)
        val sym = v.symbol
        if (!v.accepted || sym == null) {
            Feedback.error(c)
            return v.title + ": " + v.detail
        }
        if (sym.action.type == "none") {
            Feedback.success(c)
            return sym.name + " " + v.pct + "% (no action set)"
        }
        val msg = ActionRunner.run(c, sym.action)
        val ok = msg.startsWith("Done") || msg.startsWith("Flashlight on") ||
            msg.startsWith("Flashlight off")
        if (ok) Feedback.success(c) else Feedback.error(c)
        return sym.name + " " + v.pct + "%: " + msg
    }
}