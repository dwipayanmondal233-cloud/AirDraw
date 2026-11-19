package com.example.airdraw

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView

class BackupActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var withSettings: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Backup & restore", 24f, Ui.CYAN))
        root.addView(
            Ui.text(
                this,
                "Uninstalling AirDraw deletes your symbols. Every debug update needs an uninstall, " +
                    "so save a backup file first, then restore it after installing the new version.",
                14f, Ui.GRAY, false
            ),
            Ui.params(this, Ui.WRAP, 12)
        )

        withSettings = CheckBox(this)
        withSettings.text = "Include settings and calibration"
        withSettings.setTextColor(Color.WHITE)
        withSettings.isChecked = true
        root.addView(withSettings, Ui.params(this, Ui.WRAP, 16))

        val save = Ui.button(this, "Save backup file")
        save.setOnClickListener {
            val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "application/json"
            i.putExtra(Intent.EXTRA_TITLE, "airdraw-backup.json")
            try {
                startActivityForResult(i, 1)
            } catch (e: Exception) {
                status.text = "No file picker found on this phone."
            }
        }
        root.addView(save, Ui.params(this, Ui.dp(this, 56), 12))

        val restore = Ui.button(this, "Restore from backup file", "#0A8F6A")
        restore.setOnClickListener {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "*/*"
            try {
                startActivityForResult(i, 2)
            } catch (e: Exception) {
                status.text = "No file picker found on this phone."
            }
        }
        root.addView(restore, Ui.params(this, Ui.dp(this, 56), 10))

        status = Ui.text(
            this,
            "You have " + Store.getSymbols().size + " symbols now.",
            14f, Ui.PURPLE
        )
        root.addView(status, Ui.params(this, Ui.WRAP, 20))

        root.addView(
            Ui.text(
                this,
                "Restoring adds the symbols from the file. A symbol with the same name is replaced. " +
                    "Other symbols stay. Keep the file in Downloads or Documents so it survives the uninstall.",
                12f, Ui.GRAY, false
            ),
            Ui.params(this, Ui.WRAP, 16)
        )

        setContentView(root)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            if (requestCode == 1) {
                val text = Store.exportJson(withSettings.isChecked)
                contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(text.toByteArray())
                }
                status.text = "Saved " + Store.getSymbols().size + " symbols."
            } else if (requestCode == 2) {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""
                val err = Store.importJson(text, withSettings.isChecked)
                status.text = if (err == null) {
                    "Restored. You now have " + Store.getSymbols().size + " symbols."
                } else {
                    err
                }
            }
        } catch (e: Exception) {
            status.text = "Failed: " + e.message
        }
    }
}