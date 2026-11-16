package com.example.airdraw

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

class MainActivity : Activity() {

    private lateinit var listBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "AirDraw", 30f, Ui.CYAN))
        root.addView(Ui.text(this, "Draw magic in the air", 14f, Ui.PURPLE))
        root.addView(
            Ui.text(this, "My symbols (tap one to re-teach it)", 13f, Ui.GRAY),
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
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setBackgroundColor(Color.parseColor("#14142A"))

            val label = Ui.text(
                this,
                s.name + "   (" + s.samples.size + " samples)",
                20f, Ui.CYAN, false
            )
            label.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14))
            label.setOnClickListener {
                val i = Intent(this, TeachActivity::class.java)
                i.putExtra("name", s.name)
                startActivity(i)
            }

            val del = Ui.button(this, "Delete", "#AA2244")
            del.setOnClickListener { confirmDelete(s.name) }

            row.addView(label, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            row.addView(del, LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP))
            listBox.addView(row, Ui.params(this, Ui.WRAP, 6))
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