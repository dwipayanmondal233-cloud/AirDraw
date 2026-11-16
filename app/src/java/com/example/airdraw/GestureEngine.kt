package com.example.airdraw

import android.content.Context
import android.graphics.PointF
import kotlin.math.roundToInt

object GestureEngine {

    fun process(c: Context, pts: List<PointF>): String {
        if (Recognizer.isTooSmall(pts)) {
            Feedback.error(c)
            return "Too small. Draw bigger."
        }
        val symbols = Store.getSymbols()
        val r = Recognizer.recognize(pts, symbols, Store.rotInv)
        if (r == null) {
            Feedback.error(c)
            return "No symbols taught yet."
        }
        val pct = (r.score * 100).roundToInt()
        if (pct < Store.threshold) {
            Feedback.error(c)
            return "No match (closest " + r.name + " " + pct + "%)"
        }
        val sym = symbols.firstOrNull { it.name == r.name }
        if (sym == null || sym.action.type == "none") {
            Feedback.success(c)
            return r.name + " " + pct + "% (no action set)"
        }
        val msg = ActionRunner.run(c, sym.action)
        val ok = msg.startsWith("Done") || msg.startsWith("Flashlight on") ||
            msg.startsWith("Flashlight off")
        if (ok) Feedback.success(c) else Feedback.error(c)
        return r.name + " " + pct + "%: " + msg
    }
}