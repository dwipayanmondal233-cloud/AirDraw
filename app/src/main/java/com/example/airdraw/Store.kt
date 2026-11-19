package com.example.airdraw

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import org.json.JSONArray
import org.json.JSONObject

data class ActionDef(val type: String, val data: String, val label: String)

data class Symbol(
    val name: String,
    val samples: List<List<PointF>>,
    val action: ActionDef = ActionDef("none", "", "No action")
)

object Store {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext
                .getSharedPreferences("airdraw", Context.MODE_PRIVATE)
        }
    }

    var threshold: Int
        get() = prefs.getInt("threshold", 60)
        set(v) { prefs.edit().putInt("threshold", v).apply() }

    var recordMs: Int
        get() = prefs.getInt("recordMs", 1500)
        set(v) { prefs.edit().putInt("recordMs", v).apply() }

    var rotInv: Boolean
        get() = prefs.getBoolean("rotInv", false)
        set(v) { prefs.edit().putBoolean("rotInv", v).apply() }

    var flipX: Boolean
        get() = prefs.getBoolean("flipX", false)
        set(v) { prefs.edit().putBoolean("flipX", v).apply() }

    var trigger: String
        get() = prefs.getString("trigger", "shake") ?: "shake"
        set(v) { prefs.edit().putString("trigger", v).apply() }

    var shakeG: Int
        get() = prefs.getInt("shakeG", 25)
        set(v) { prefs.edit().putInt("shakeG", v).apply() }

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) { prefs.edit().putBoolean("vibration", v).apply() }

    var sounds: Boolean
        get() = prefs.getBoolean("sounds", true)
        set(v) { prefs.edit().putBoolean("sounds", v).apply() }

    var margin: Int
        get() = prefs.getInt("margin", 6)
        set(v) { prefs.edit().putInt("margin", v).apply() }

    var minSize: Int
        get() = prefs.getInt("minSize", 12)
        set(v) { prefs.edit().putInt("minSize", v).apply() }

    var cooldownMs: Int
        get() = prefs.getInt("cooldownMs", 2000)
        set(v) { prefs.edit().putInt("cooldownMs", v).apply() }

    var shakeDelay: Int
        get() = prefs.getInt("shakeDelay", 450)
        set(v) { prefs.edit().putInt("shakeDelay", v).apply() }

    var deadZone: Int
        get() = prefs.getInt("deadZone", 15)
        set(v) { prefs.edit().putInt("deadZone", v).apply() }

    var callExtra: Int
        get() = prefs.getInt("callExtra", 10)
        set(v) { prefs.edit().putInt("callExtra", v).apply() }

    var tilt: Int
        get() = prefs.getInt("tilt", 10)
        set(v) { prefs.edit().putInt("tilt", v).apply() }

    var screenOnly: Boolean
        get() = prefs.getBoolean("screenOnly", false)
        set(v) { prefs.edit().putBoolean("screenOnly", v).apply() }

    var calibrated: Boolean
        get() = prefs.getBoolean("calibrated", false)
        set(v) { prefs.edit().putBoolean("calibrated", v).apply() }

    var bias0: Float
        get() = prefs.getFloat("bias0", 0f)
        set(v) { prefs.edit().putFloat("bias0", v).apply() }

    var bias1: Float
        get() = prefs.getFloat("bias1", 0f)
        set(v) { prefs.edit().putFloat("bias1", v).apply() }

    fun resetSettings() {
        val e = prefs.edit()
        val keys = listOf(
            "threshold", "recordMs", "rotInv", "flipX", "flipY", "trigger", "shakeG",
            "vibration", "sounds", "margin", "minSize", "cooldownMs", "shakeDelay",
            "deadZone", "callExtra", "tilt", "screenOnly", "calibrated", "bias0", "bias1"
        )
        for (k in keys) e.remove(k)
        e.apply()
    }

    fun getSymbol(name: String): Symbol? {
        return getSymbols().firstOrNull { it.name == name }
    }

    fun addSample(name: String, sample: List<PointF>) {
        val list = ArrayList<Symbol>()
        for (s in getSymbols()) {
            if (s.name == name) {
                val ns = ArrayList<List<PointF>>(s.samples)
                ns.add(sample)
                list.add(Symbol(s.name, ns, s.action))
            } else {
                list.add(s)
            }
        }
        saveAll(list)
    }

    fun removeSample(name: String, index: Int) {
        val list = ArrayList<Symbol>()
        for (s in getSymbols()) {
            if (s.name == name && index >= 0 && index < s.samples.size) {
                val ns = ArrayList<List<PointF>>(s.samples)
                ns.removeAt(index)
                list.add(Symbol(s.name, ns, s.action))
            } else {
                list.add(s)
            }
        }
        saveAll(list)
    }

    fun addLog(ok: Boolean, text: String) {
        try {
            val arr = JSONArray(prefs.getString("log", "[]"))
            val out = JSONArray()
            val start = Math.max(0, arr.length() - 39)
            for (i in start until arr.length()) out.put(arr.get(i))
            val o = JSONObject()
            o.put("t", System.currentTimeMillis())
            o.put("ok", ok)
            o.put("s", text)
            out.put(o)
            prefs.edit().putString("log", out.toString()).apply()
        } catch (e: Exception) {
        }
    }

    fun getLog(): List<LogEntry> {
        val out = ArrayList<LogEntry>()
        try {
            val arr = JSONArray(prefs.getString("log", "[]"))
            for (i in arr.length() - 1 downTo 0) {
                val o = arr.getJSONObject(i)
                out.add(LogEntry(o.getLong("t"), o.getBoolean("ok"), o.getString("s")))
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun clearLog() {
        prefs.edit().remove("log").apply()
    }

    private val settingKeys = listOf(
        "threshold", "recordMs", "rotInv", "flipX", "flipY", "trigger", "shakeG",
        "vibration", "sounds", "margin", "minSize", "cooldownMs", "shakeDelay",
        "deadZone", "callExtra", "tilt", "screenOnly", "calibrated", "bias0", "bias1",
        "tapLevel"
    )

    var tapLevel: Int
        get() = prefs.getInt("tapLevel", 5)
        set(v) { prefs.edit().putInt("tapLevel", v).apply() }

    // jerk needed for a back tap. Higher level = easier.
    fun tapThreshold(): Float = 1100f - 90f * tapLevel

    fun hasTrigger(t: String): Boolean {
        val v = trigger
        if (v == "both") return t == "shake" || t == "volume"
        return v.split(",").contains(t)
    }

    fun setTrigger(t: String, on: Boolean) {
        val cur = ArrayList<String>()
        for (x in listOf("shake", "volume", "tap")) {
            if (hasTrigger(x)) cur.add(x)
        }
        cur.remove(t)
        if (on) cur.add(t)
        trigger = cur.joinToString(",")
    }

    fun triggerLabel(): String {
        val names = ArrayList<String>()
        if (hasTrigger("shake")) names.add("Shake")
        if (hasTrigger("volume")) names.add("Volume Down")
        if (hasTrigger("tap")) names.add("Back double-tap")
        return if (names.isEmpty()) "None" else names.joinToString(" + ")
    }

    fun resetAll() {
        resetSettings()
        prefs.edit().remove("tapLevel").apply()
    }

    private fun parseSymbols(arr: JSONArray): List<Symbol> {
        val out = ArrayList<Symbol>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sj = o.getJSONArray("samples")
            val samples = ArrayList<List<PointF>>()
            for (j in 0 until sj.length()) {
                val pj = sj.getJSONArray(j)
                val pts = ArrayList<PointF>()
                for (k in 0 until pj.length()) {
                    val p = pj.getJSONArray(k)
                    pts.add(PointF(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()))
                }
                samples.add(pts)
            }
            val aj = o.optJSONObject("action")
            val action = if (aj != null) {
                ActionDef(
                    aj.optString("type", "none"),
                    aj.optString("data", ""),
                    aj.optString("label", "No action")
                )
            } else {
                ActionDef("none", "", "No action")
            }
            out.add(Symbol(o.getString("name"), samples, action))
        }
        return out
    }

    fun exportJson(withSettings: Boolean): String {
        val o = JSONObject()
        o.put("app", "airdraw")
        o.put("version", 1)
        val syms = try {
            JSONArray(prefs.getString("symbols", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        o.put("symbols", syms)
        if (withSettings) {
            val st = JSONObject()
            for (k in settingKeys) {
                val v = prefs.all[k]
                if (v != null) st.put(k, v)
            }
            o.put("settings", st)
        }
        return o.toString()
    }

    // returns null when it worked, or an error message
    fun importJson(text: String, withSettings: Boolean): String? {
        try {
            val o = JSONObject(text)
            if (o.optString("app") != "airdraw") return "This is not an AirDraw backup."
            val incoming = parseSymbols(o.getJSONArray("symbols"))
            val names = HashSet<String>()
            for (s in incoming) names.add(s.name)
            val merged = ArrayList<Symbol>()
            for (s in getSymbols()) {
                if (!names.contains(s.name)) merged.add(s)
            }
            merged.addAll(incoming)
            saveAll(merged)

            if (withSettings && o.has("settings")) {
                val st = o.getJSONObject("settings")
                val e = prefs.edit()
                val keys = st.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!settingKeys.contains(k)) continue
                    val v = st.get(k)
                    if (v is Boolean) {
                        e.putBoolean(k, v)
                    } else if (v is String) {
                        e.putString(k, v)
                    } else if (v is Number) {
                        if (k == "bias0" || k == "bias1") {
                            e.putFloat(k, v.toFloat())
                        } else {
                            e.putInt(k, v.toInt())
                        }
                    }
                }
                e.apply()
            }
            return null
        } catch (ex: Exception) {
            return "Could not read that file."
        }
    }

    var flipY: Boolean
        get() = prefs.getBoolean("flipY", false)
        set(v) { prefs.edit().putBoolean("flipY", v).apply() }

    fun getSymbols(): List<Symbol> {
        val out = ArrayList<Symbol>()
        val s = prefs.getString("symbols", null) ?: return out
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sj = o.getJSONArray("samples")
                val samples = ArrayList<List<PointF>>()
                for (j in 0 until sj.length()) {
                    val pj = sj.getJSONArray(j)
                    val pts = ArrayList<PointF>()
                    for (k in 0 until pj.length()) {
                        val p = pj.getJSONArray(k)
                        pts.add(PointF(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()))
                    }
                    samples.add(pts)
                }
                val aj = o.optJSONObject("action")
                val action = if (aj != null) {
                    ActionDef(
                        aj.optString("type", "none"),
                        aj.optString("data", ""),
                        aj.optString("label", "No action")
                    )
                } else {
                    ActionDef("none", "", "No action")
                }
                out.add(Symbol(o.getString("name"), samples, action))
            }
        } catch (e: Exception) {
            return ArrayList()
        }
        return out
    }

    private fun saveAll(list: List<Symbol>) {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("name", s.name)
            val sj = JSONArray()
            for (sample in s.samples) {
                val pj = JSONArray()
                for (p in sample) {
                    val pair = JSONArray()
                    pair.put(p.x.toDouble())
                    pair.put(p.y.toDouble())
                    pj.put(pair)
                }
                sj.put(pj)
            }
            o.put("samples", sj)
            val aj = JSONObject()
            aj.put("type", s.action.type)
            aj.put("data", s.action.data)
            aj.put("label", s.action.label)
            o.put("action", aj)
            arr.put(o)
        }
        prefs.edit().putString("symbols", arr.toString()).apply()
    }

    fun saveSymbol(name: String, samples: List<List<PointF>>) {
        val list = ArrayList<Symbol>()
        var keep = ActionDef("none", "", "No action")
        for (s in getSymbols()) {
            if (s.name == name) {
                keep = s.action
            } else {
                list.add(s)
            }
        }
        list.add(Symbol(name, samples, keep))
        saveAll(list)
    }

    fun setAction(name: String, action: ActionDef) {
        val list = ArrayList<Symbol>()
        for (s in getSymbols()) {
            if (s.name == name) {
                list.add(Symbol(s.name, s.samples, action))
            } else {
                list.add(s)
            }
        }
        saveAll(list)
    }

    fun deleteSymbol(name: String) {
        val list = ArrayList<Symbol>()
        for (s in getSymbols()) {
            if (s.name != name) list.add(s)
        }
        saveAll(list)
    }
}