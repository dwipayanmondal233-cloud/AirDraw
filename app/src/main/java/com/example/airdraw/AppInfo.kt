package com.example.airdraw

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView

class AppItem(val label: String, val pkg: String)

object AppInfo {

    fun label(c: Context, pkg: String): String? {
        return try {
            val pm = c.packageManager
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun icon(c: Context, pkg: String): Drawable? {
        return try {
            c.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            null
        }
    }

    fun loadAll(c: Context): List<AppItem> {
        val pm = c.packageManager
        val q = Intent(Intent.ACTION_MAIN)
        q.addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (r in pm.queryIntentActivities(q, 0)) {
            val pkg = r.activityInfo.packageName
            if (pkg == c.packageName) continue
            if (!seen.add(pkg)) continue
            out.add(AppItem(r.loadLabel(pm).toString(), pkg))
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    // Shows a searchable list with icons and names.
    fun pick(a: Activity, onPick: (AppItem) -> Unit) {
        val all = loadAll(a)
        if (all.isEmpty()) return
        var shown: List<AppItem> = all
        val icons = HashMap<String, Drawable?>()

        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL

        val search = EditText(a)
        search.hint = "Search apps"
        search.setHintTextColor(Color.parseColor("#888899"))
        search.setTextColor(Color.WHITE)
        search.isSingleLine = true
        box.addView(search)

        val list = ListView(a)
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = shown.size

            override fun getItem(position: Int): Any = shown[position]

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val item = shown[position]
                val row = LinearLayout(a)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8))

                val iv = ImageView(a)
                iv.setImageDrawable(icons.getOrPut(item.pkg) { icon(a, item.pkg) })
                row.addView(iv, LinearLayout.LayoutParams(Ui.dp(a, 40), Ui.dp(a, 40)))

                val col = LinearLayout(a)
                col.orientation = LinearLayout.VERTICAL
                col.addView(Ui.text(a, item.label, 16f, "#FFFFFF", false))
                col.addView(Ui.text(a, item.pkg, 11f, Ui.GRAY, false))
                val lp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
                lp.leftMargin = Ui.dp(a, 12)
                row.addView(col, lp)
                return row
            }
        }
        list.adapter = adapter
        box.addView(
            list,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (a.resources.displayMetrics.heightPixels * 0.55).toInt()
            )
        )

        val dialog = AlertDialog.Builder(a)
            .setTitle("Choose an app")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .create()

        list.setOnItemClickListener { _, _, pos, _ ->
            onPick(shown[pos])
            dialog.dismiss()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                shown = if (q.isEmpty()) all else all.filter { it.label.lowercase().contains(q) }
                adapter.notifyDataSetChanged()
            }
        })

        dialog.show()
    }
}