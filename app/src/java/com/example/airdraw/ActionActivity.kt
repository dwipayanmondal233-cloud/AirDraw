package com.example.airdraw

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

class ActionActivity : Activity() {

    private var symbolName = ""
    private lateinit var group: RadioGroup
    private lateinit var appBtn: Button
    private lateinit var appInfo: TextView
    private lateinit var phoneBox: EditText
    private lateinit var urlBox: EditText
    private lateinit var info: TextView

    private val types = listOf("none", "flashlight", "app", "camera", "call", "url", "media")
    private val names = listOf(
        "No action", "Flashlight toggle", "Open an app", "Open camera",
        "Call a number", "Open a URL", "Media play/pause"
    )
    private val ids = ArrayList<Int>()
    private var pickedPkg = ""
    private var pickedLabel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ActionRunner.init(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        symbolName = intent.getStringExtra("name") ?: ""
        val existing = Store.getSymbols().firstOrNull { it.name == symbolName }?.action

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Action for: " + symbolName, 24f, Ui.CYAN))
        root.addView(
            Ui.text(this, "What should this symbol do?", 13f, Ui.PURPLE),
            Ui.params(this, Ui.WRAP, 4)
        )

        group = RadioGroup(this)
        for (i in types.indices) {
            val rb = RadioButton(this)
            rb.text = names[i]
            rb.setTextColor(Color.WHITE)
            rb.id = View.generateViewId()
            ids.add(rb.id)
            group.addView(rb)
        }
        root.addView(group, Ui.params(this, Ui.WRAP, 12))

        appBtn = Ui.button(this, "Choose app", "#444466")
        appBtn.setOnClickListener { pickApp() }
        appInfo = Ui.text(this, "No app selected", 13f, Ui.GRAY, false)

        phoneBox = EditText(this)
        phoneBox.hint = "Phone number, for example +911234567890"
        phoneBox.setHintTextColor(Color.parseColor("#666688"))
        phoneBox.setTextColor(Color.WHITE)
        phoneBox.inputType = InputType.TYPE_CLASS_PHONE

        urlBox = EditText(this)
        urlBox.hint = "https://example.com"
        urlBox.setHintTextColor(Color.parseColor("#666688"))
        urlBox.setTextColor(Color.WHITE)
        urlBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        root.addView(appBtn, Ui.params(this, Ui.dp(this, 52), 8))
        root.addView(appInfo, Ui.params(this, Ui.WRAP, 4))
        root.addView(phoneBox, Ui.params(this, Ui.WRAP, 8))
        root.addView(urlBox, Ui.params(this, Ui.WRAP, 8))

        info = Ui.text(this, "", 14f, Ui.GRAY)
        root.addView(info, Ui.params(this, Ui.WRAP, 12))

        val testBtn = Ui.button(this, "Test action", "#444466")
        testBtn.setOnClickListener { testAction() }
        val saveBtn = Ui.button(this, "Save action", "#0A8F6A")
        saveBtn.setOnClickListener { saveAction() }

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val tLp = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        tLp.rightMargin = Ui.dp(this, 4)
        val sLp = LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1f)
        sLp.leftMargin = Ui.dp(this, 4)
        row.addView(testBtn, tLp)
        row.addView(saveBtn, sLp)
        root.addView(row, Ui.params(this, Ui.WRAP, 8))

        root.addView(
            Ui.text(
                this,
                "Careful: Call dials at once. Use a higher match threshold so a wrong symbol never calls anyone.",
                12f, Ui.PURPLE, false
            ),
            Ui.params(this, Ui.WRAP, 16)
        )

        group.setOnCheckedChangeListener { _, _ -> updateVisibility() }

        if (existing != null) {
            val idx = types.indexOf(existing.type)
            group.check(ids[if (idx < 0) 0 else idx])
            if (existing.type == "app") {
                pickedPkg = existing.data
                pickedLabel = existing.label.removePrefix("Open ")
                appInfo.text = "Selected: " + pickedLabel
            }
            if (existing.type == "call") phoneBox.setText(existing.data)
            if (existing.type == "url") urlBox.setText(existing.data)
        } else {
            group.check(ids[0])
        }
        updateVisibility()

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Ui.BG))
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun selectedType(): String {
        val i = ids.indexOf(group.checkedRadioButtonId)
        return if (i < 0) "none" else types[i]
    }

    private fun updateVisibility() {
        val t = selectedType()
        appBtn.visibility = if (t == "app") View.VISIBLE else View.GONE
        appInfo.visibility = if (t == "app") View.VISIBLE else View.GONE
        phoneBox.visibility = if (t == "call") View.VISIBLE else View.GONE
        urlBox.visibility = if (t == "url") View.VISIBLE else View.GONE
        info.text = ""
    }

    private fun pickApp() {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN)
        q.addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(q, 0)
        val items = ArrayList<Pair<String, String>>()
        for (r in list) {
            val pkg = r.activityInfo.packageName
            if (pkg == packageName) continue
            items.add(Pair(r.loadLabel(pm).toString(), pkg))
        }
        items.sortBy { it.first.lowercase() }
        if (items.isEmpty()) {
            info.text = "No apps found."
            return
        }
        val labels = items.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose an app")
            .setItems(labels) { _, which ->
                pickedLabel = items[which].first
                pickedPkg = items[which].second
                appInfo.text = "Selected: " + pickedLabel
            }
            .show()
    }

    private fun buildAction(): ActionDef? {
        return when (selectedType()) {
            "flashlight" -> ActionDef("flashlight", "", "Flashlight toggle")
            "camera" -> ActionDef("camera", "", "Open camera")
            "media" -> ActionDef("media", "", "Media play/pause")
            "app" -> {
                if (pickedPkg.isEmpty()) {
                    info.text = "Choose an app first."
                    null
                } else {
                    ActionDef("app", pickedPkg, "Open " + pickedLabel)
                }
            }
            "call" -> {
                val n = phoneBox.text.toString().filter {
                    it.isDigit() || it == '+' || it == '*' || it == '#'
                }
                if (n.isEmpty()) {
                    info.text = "Type a phone number."
                    null
                } else {
                    ActionDef("call", n, "Call " + n)
                }
            }
            "url" -> {
                val u = urlBox.text.toString().trim()
                if (u.isEmpty()) {
                    info.text = "Type a web address."
                    null
                } else {
                    ActionDef("url", u, "Open " + u)
                }
            }
            else -> ActionDef("none", "", "No action")
        }
    }

    private fun hasCallPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun askCallPermission(): Boolean {
        if (hasCallPermission()) return true
        requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
        info.text = "Allow the call permission, then tap the button again."
        return false
    }

    private fun testAction() {
        val a = buildAction() ?: return
        if (a.type == "call" && !askCallPermission()) return
        info.text = ActionRunner.run(this, a)
    }

    private fun saveAction() {
        val a = buildAction() ?: return
        if (a.type == "call" && !askCallPermission()) return
        Store.setAction(symbolName, a)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            info.text = "Permission allowed. Tap the button again."
        } else {
            info.text = "Permission denied. Allow it in Settings, Apps, AirDraw, Permissions."
        }
    }
}