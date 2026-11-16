package com.example.airdraw

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

class MainActivity : Activity() {

    private lateinit var listBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ActionRunner.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "AirDraw", 30f, Ui.CYAN))
        root.addView(Ui.text(this, "Draw magic in the air", 14f, Ui.PURPLE))
        root.addView(
            Ui.text(this, "My symbols", 13f, Ui.GRAY),
            Ui.params(this, Ui.WRAP, 14)
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
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val pLp = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        pLp.rightMargin = Ui.dp(this, 4)
        val sLp = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        sLp.leftMargin = Ui.dp(this, 4)
        row.addView(practice, pLp)
        row.addView(settings, sLp)
        root.addView(row, Ui.params(this, Ui.WRAP, 8))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
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

            val a = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            a.rightMargin = Ui.dp(this, 3)
            val b = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            b.leftMargin = Ui.dp(this, 3)
            b.rightMargin = Ui.dp(this, 3)
            val c = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            c.leftMargin = Ui.dp(this, 3)
            btnRow.addView(actionBtn, a)
            btnRow.addView(teachBtn, b)
            btnRow.addView(delBtn, c)
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