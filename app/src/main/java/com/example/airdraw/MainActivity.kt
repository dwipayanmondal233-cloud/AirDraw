package com.example.airdraw

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
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

        val practice = Ui.button(this, "Practice")
        practice.setOnClickListener {
            startActivity(Intent(this, PracticeActivity::class.java))
        }
        val settings = Ui.button(this, "Settings")
        settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val help = Ui.button(this, "Help")
        help.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val a = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        a.rightMargin = Ui.dp(this, 3)
        val b = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        b.leftMargin = Ui.dp(this, 3)
        b.rightMargin = Ui.dp(this, 3)
        val c = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        c.leftMargin = Ui.dp(this, 3)
        row.addView(practice, a)
        row.addView(settings, b)
        row.addView(help, c)
        root.addView(row, Ui.params(this, Ui.WRAP, 8))

        setContentView(root)
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
        status.text = "Service: " + svc + "\nTrigger: " + trig + "\nAccessibility: " + acc
    }

    private fun smallBtn(text: String, color: String, onClick: () -> Unit): Button {
        val b = Ui.button(this, text, color)
        b.setOnClickListener { onClick() }
        return b
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

            card.addView(Ui.text(this, s.name, 22f, Ui.CYAN, false))
            card.addView(
                Ui.text(
                    this,
                    "Action: " + s.action.label + "  |  " + s.samples.size + " samples",
                    13f, Ui.GRAY, false
                )
            )

            val btnRow = LinearLayout(this)
            btnRow.orientation = LinearLayout.HORIZONTAL
            val actionBtn = smallBtn("Action", Ui.BTN) {
                val i = Intent(this, ActionActivity::class.java)
                i.putExtra("name", s.name)
                startActivity(i)
            }
            val teachBtn = smallBtn("Re-teach", "#444466") {
                val i = Intent(this, TeachActivity::class.java)
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
            btnRow.addView(teachBtn, p2)
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