package com.example.airdraw

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

class ActionActivity : Activity() {

    private var symbolName = ""
    private lateinit var group: RadioGroup
    private lateinit var appBtn: Button
    private lateinit var appRow: LinearLayout
    private lateinit var appIcon: ImageView
    private lateinit var appName: TextView
    private lateinit var appPkg: TextView
    private lateinit var phoneBox: EditText
    private lateinit var urlBox: EditText
    private lateinit var callNote: TextView
    private lateinit var info: TextView

    private val types = listOf("none", "flashlight", "app", "camera", "call", "url", "media")
    private val names = listOf(
        "No action", "Flashlight toggle", "Open an app", "Open camera",
        "Call a number", "Open a URL", "Media play/pause"
    )
    private val ids = ArrayList<Int>()
    private var pickedPkg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ActionRunner.init(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        symbolName = intent.getStringExtra("name") ?: ""
        val existing = Store.getSymbol(symbolName)?.action

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

        // App picker + selected app display
        appBtn = Ui.button(this, "Choose app", "#444466")
        appBtn.setOnClickListener {
            AppInfo.pick(this) { item ->
                pickedPkg = item.pkg
                showPicked()
            }
        }

        appRow = LinearLayout(this)
        appRow.orientation = LinearLayout.HORIZONTAL
        appRow.gravity = Gravity.CENTER_VERTICAL
        appRow.setBackgroundColor(Color.parseColor("#14142A"))
        appRow.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10))
        appIcon = ImageView(this)
        appRow.addView(appIcon, LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        appName = Ui.text(this, "", 18f, Ui.CYAN, false)
        appPkg = Ui.text(this, "", 11f, Ui.GRAY, false)
        col.addView(appName)
        col.addView(appPkg)
        val colLp = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        colLp.leftMargin = Ui.dp(this, 12)
        appRow.addView(col, colLp)

        phoneBox = EditText(this)
        phoneBox.hint = "Phone number, for example +911234567890"
        phoneBox.setHintTextColor(Color.parseColor("#666688"))
        phoneBox.setTextColor(Color.WHITE)
        phoneBox.inputType = InputType.TYPE_CLASS_PHONE

        callNote = Ui.text(
            this,
            "Safety: Call symbols need " + Store.callExtra + "% more score than other symbols.",
            12f, Ui.PURPLE, false
        )

        urlBox = EditText(this)
        urlBox.hint = "https://example.com"
        urlBox.setHintTextColor(Color.parseColor("#666688"))
        urlBox.setTextColor(Color.WHITE)
        urlBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        root.addView(appBtn, Ui.params(this, Ui.dp(this, 52), 8))
        root.addView(appRow, Ui.params(this, Ui.WRAP, 8))
        root.addView(phoneBox, Ui.params(this, Ui.WRAP, 8))
        root.addView(callNote, Ui.params(this, Ui.WRAP, 4))
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

        group.setOnCheckedChangeListener { _, _ -> updateVisibility() }

        if (existing != null) {
            val idx = types.indexOf(existing.type)
            group.check(ids[if (idx < 0) 0 else idx])
            if (existing.type == "app") pickedPkg = existing.data
            if (existing.type == "call") phoneBox.setText(existing.data)
            if (existing.type == "url") urlBox.setText(existing.data)
        } else {
            group.check(ids[0])
        }
        showPicked()
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

    private fun showPicked() {
        if (pickedPkg.isEmpty()) {
            appName.text = "No app selected"
            appPkg.text = ""
            appIcon.setImageDrawable(null)
            appBtn.text = "Choose app"
            return
        }
        val label = AppInfo.label(this, pickedPkg)
        if (label == null) {
            appName.text = "\u2715 App not found"
            appPkg.text = pickedPkg
            appIcon.setImageDrawable(null)
        } else {
            appName.text = label
            appPkg.text = pickedPkg
            appIcon.setImageDrawable(AppInfo.icon(this, pickedPkg))
        }
        appBtn.text = "Change app"
    }

    private fun updateVisibility() {
        val t = selectedType()
        appBtn.visibility = if (t == "app") View.VISIBLE else View.GONE
        appRow.visibility = if (t == "app") View.VISIBLE else View.GONE
        phoneBox.visibility = if (t == "call") View.VISIBLE else View.GONE
        callNote.visibility = if (t == "call") View.VISIBLE else View.GONE
        urlBox.visibility = if (t == "url") View.VISIBLE else View.GONE
        info.text = ""
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
                    val label = AppInfo.label(this, pickedPkg)
                    if (label == null) {
                        info.text = "That app is not installed. Choose another."
                        null
                    } else {
                        ActionDef("app", pickedPkg, "Open " + label)
                    }
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

    private fun askCallPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        ) return true
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
            info.text = "Permission denied. Use Permissions on the main screen to fix it."
        }
    }
}