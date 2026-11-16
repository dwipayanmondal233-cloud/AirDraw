package com.example.airdraw

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var listBox: LinearLayout
    private lateinit var master: Switch
    private lateinit var status: TextView

    private val masterListener = CompoundButton.OnCheckedChangeListener { _, on ->
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            }
            AirService.start(this)
        } else {
            AirService.stop(this)
        }
        status.postDelayed({ updateStatus() }, 500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ActionRunner.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "AirDraw", 30f, Ui.CYAN))
        root.addView(Ui.text(this, "Draw magic in the air", 14f, Ui.PURPLE))

        master = Switch(this)
        master.text = "AirDraw ON"
        master.textSize = 18f
        master.setTextColor(Color.WHITE)
        root.addView(master, Ui.params(this, Ui.WRAP, 14))

        status = Ui.text(this, "", 12f, Ui.GRAY, false)
        status.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
        root.addView(status, Ui.params(this, Ui.WRAP, 4))

        root.addView(
            Ui.text(this, "My symbols", 13f, Ui.GRAY),
            Ui.params(this, Ui.WRAP, 12)
        )

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.addView(listBox)
        root.addView(scroll, Ui.params(this, 0, 6, 1f))

        val teach = Ui.button(this, "Teach new symbol")
        teach.setOnClickListener {
            startActivity(Intent(this, TeachActivity::class.java))
        }
        root.addView(teach, Ui.params(this, Ui.dp(this, 56), 8))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val b1 = compact(Ui.button(this, "Practice"))
        b1.setOnClickListener { startActivity(Intent(this, PracticeActivity::class.java)) }
        val b2 = compact(Ui.button(this, "Settings"))
        b2.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        val b3 = compact(Ui.button(this, "History"))
        b3.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        val b4 = compact(Ui.button(this, "Help"))
        b4.setOnClickListener { startActivity(Intent(this, HelpActivity::class.java)) }
        for (b in listOf(b1, b2, b3, b4)) {
            val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f)
            lp.leftMargin = Ui.dp(this, 2)
            lp.rightMargin = Ui.dp(this, 2)
            row.addView(b, lp)
        }
        root.addView(row, Ui.params(this, Ui.WRAP, 8))

        setContentView(root)
    }

    private fun compact(b: Button): Button {
        b.minWidth = 0
        b.minimumWidth = 0
        b.textSize = 13f
        b.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0)
        return b
    }

    override fun onResume() {
        super.onResume()
        master.setOnCheckedChangeListener(null)
        master.isChecked = AirService.running
        master.setOnCheckedChangeListener(masterListener)
        updateStatus()
        refresh()
    }

    private fun updateStatus() {
        val trig = when (Store.trigger) {
            "volume" -> "Hold Volume Down"
            "both" -> "Shake or Volume Down"
            else -> "Shake"
        }
        val svc = if (AirService.running) "running" else "off"
        val acc = if (KeyService.connected) "on" else "off"
        status.text = "Service: " + svc + "  |  Trigger: " + trig + "  |  Accessibility: " + acc +
            "\nPermissions: " + Perms.summary(this) + " ready (tap to check)"
    }

    private fun smallBtn(text: String, color: String, onClick: () -> Unit): Button {
        val b = Ui.button(this, text, color)
        b.setOnClickListener { onClick() }
        return b
    }

    private fun actionLine(s: Symbol): LinearLayout {
        val line = LinearLayout(this)
        line.orientation = LinearLayout.HORIZONTAL
        line.gravity = Gravity.CENTER_VERTICAL
        val a = s.action
        var text = a.label
        var color = Ui.GRAY

        if (a.type == "app") {
            val label = AppInfo.label(this, a.data)
            val icon = AppInfo.icon(this, a.data)
            if (label != null && icon != null) {
                val iv = ImageView(this)
                iv.setImageDrawable(icon)
                line.addView(iv, LinearLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22)))
                text = "  Open " + label
            } else {
                text = "\u2715 App missing: " + a.data
                color = "#FF4466"
            }
        } else if (a.type == "none") {
            text = "No action. Tap Action to set one."
            color = "#FFB020"
        }
        line.addView(Ui.text(this, text, 13f, color, false))
        return line
    }

    private fun refresh() {
        listBox.removeAllViews()
        val symbols = Store.getSymbols()
        if (symbols.isEmpty()) {
            listBox.addView(
                Ui.text(this, "No symbols yet. Tap Teach new symbol.", 14f, Ui.GRAY),
                Ui.params(this, Ui.WRAP, 20)
            )
            return
        }
        for (s in symbols) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(Color.parseColor("#14142A"))
            card.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10))

            val top = LinearLayout(this)
            top.orientation = LinearLayout.HORIZONTAL
            top.gravity = Gravity.CENTER_VERTICAL

            val thumb = MiniPathView(this)
            if (s.samples.isNotEmpty()) thumb.setPath(s.samples[0])
            top.addView(thumb, LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)))

            val info = LinearLayout(this)
            info.orientation = LinearLayout.VERTICAL
            info.addView(Ui.text(this, s.name, 22f, Ui.CYAN, false))
            info.addView(actionLine(s))
            info.addView(Ui.text(this, s.samples.size.toString() + " samples", 12f, Ui.GRAY, false))
            val infoLp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
            infoLp.leftMargin = Ui.dp(this, 12)
            top.addView(info, infoLp)
            card.addView(top)

            val btnRow = LinearLayout(this)
            btnRow.orientation = LinearLayout.HORIZONTAL
            val actionBtn = smallBtn("Action", Ui.BTN) {
                val i = Intent(this, ActionActivity::class.java)
                i.putExtra("name", s.name)
                startActivity(i)
            }
            val samplesBtn = smallBtn("Samples", "#444466") {
                val i = Intent(this, SamplesActivity::class.java)
                i.putExtra("name", s.name)
                startActivity(i)
            }
            val delBtn = smallBtn("Delete", "#AA2244") { confirmDelete(s.name) }

            val p1 = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            p1.rightMargin = Ui.dp(this, 3)
            val p2 = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            p2.leftMargin = Ui.dp(this, 3)
            p2.rightMargin = Ui.dp(this, 3)
            val p3 = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            p3.leftMargin = Ui.dp(this, 3)
            btnRow.addView(actionBtn, p1)
            btnRow.addView(samplesBtn, p2)
            btnRow.addView(delBtn, p3)
            card.addView(btnRow, Ui.params(this, Ui.WRAP, 8))

            listBox.addView(card, Ui.params(this, Ui.WRAP, 8))
        }
    }

    private fun confirmDelete(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete " + name + "?")
            .setPositiveButton("Delete") { _, _ ->
                Store.deleteSymbol(name)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}