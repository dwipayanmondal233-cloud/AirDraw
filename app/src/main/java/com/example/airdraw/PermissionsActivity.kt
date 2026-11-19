package com.example.airdraw

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PermissionsActivity : Activity() {

    private lateinit var box: LinearLayout
    private lateinit var summary: TextView
    private var denied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Permissions", 26f, Ui.CYAN))
        summary = Ui.text(this, "", 14f, Ui.GRAY)
        root.addView(summary, Ui.params(this, Ui.WRAP, 6))

        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.addView(box)
        root.addView(scroll, Ui.params(this, 0, 10, 1f))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        box.removeAllViews()
        summary.text = Perms.summary(this) + " ready" +
            (if (denied) "\nPermission denied. Use App info at the bottom." else "")

        val volume = Store.hasTrigger("volume")
        val symbols = Store.getSymbols()
        val usesCall = symbols.any { it.action.type == "call" }
        val usesFlash = symbols.any { it.action.type == "flashlight" }

        row("Gyroscope", Perms.gyro(this),
            "Needed. The motion sensor AirDraw reads.", null, null)

        row("Notifications", Perms.notifications(this),
            "Shows the service notification and the last result.", "Allow") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        row("Service running", AirService.running,
            "Turn the AirDraw switch ON on the main screen.", "Start") {
            AirService.start(this)
            box.postDelayed({ refresh() }, 700)
        }

        row("Accessibility", KeyService.connected,
            (if (volume) "Needed for the Volume Down trigger. " else "Optional. Helps open apps from the background. ") +
                "Greyed out? App info, 3-dot menu, Allow restricted settings.",
            "Open") {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        row("Call permission", Perms.call(this),
            if (usesCall) "Needed for your Call symbols." else "Only needed if you use a Call symbol.",
            "Allow") {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 2)
        }

        row("Overlay permission", Perms.overlay(this),
            "Optional backup for opening apps from the background.", "Open") {
            open(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
        }

        row("Battery unrestricted", Perms.battery(this),
            "Recommended so the phone does not stop the service.", "Open") {
            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        row("Flashlight hardware", Perms.flash(this),
            if (usesFlash) "Needed for your Flashlight symbols." else "Only needed for Flashlight symbols.",
            null, null)

        val info = Ui.button(this, "App info (all permissions)", "#444466")
        info.setOnClickListener {
            open(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + packageName)
                )
            )
        }
        box.addView(info, Ui.params(this, Ui.dp(this, 52), 16))

        box.addView(
            Ui.text(
                this,
                "No popup appears? Stop any app that draws over the screen (like a floating bubble), or allow the permission in App info.",
                12f, Ui.PURPLE, false
            ),
            Ui.params(this, Ui.WRAP, 12)
        )
    }

    private fun row(
        title: String, ok: Boolean, note: String,
        fixText: String?, fix: (() -> Unit)?
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setBackgroundColor(Color.parseColor("#14142A"))
        card.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10))

        val badge = Ui.text(
            this,
            if (ok) "\u2713" else "\u2715",
            26f,
            if (ok) "#00FF88" else "#FF4466"
        )
        card.addView(badge, LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.WRAP))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(Ui.text(this, title, 16f, "#FFFFFF", false))
        col.addView(Ui.text(this, note, 12f, Ui.GRAY, false))
        card.addView(col, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))

        if (!ok && fixText != null && fix != null) {
            val b = Ui.button(this, fixText, "#7B2FFF")
            b.minWidth = 0
            b.minimumWidth = 0
            b.textSize = 13f
            b.setOnClickListener { fix() }
            val lp = LinearLayout.LayoutParams(Ui.WRAP, Ui.dp(this, 44))
            lp.leftMargin = Ui.dp(this, 8)
            card.addView(b, lp)
        }

        box.addView(card, Ui.params(this, Ui.WRAP, 8))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        denied = !(grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED)
        refresh()
    }

    private fun open(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}