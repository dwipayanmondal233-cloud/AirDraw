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