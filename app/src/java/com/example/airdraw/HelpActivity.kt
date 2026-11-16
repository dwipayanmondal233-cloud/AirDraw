package com.example.airdraw

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

class HelpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = Ui.root(this)
        root.addView(Ui.text(this, "Help", 26f, Ui.CYAN))

        add(
            root, "How to use",
            "1. Turn the AirDraw switch ON.\n" +
                "2. Shake the phone 3 times (or hold Volume Down).\n" +
                "3. Feel a short buzz, then draw your symbol in the air.\n" +
                "4. Two pulses = it worked. One long soft buzz = no match.\n" +
                "The notification shows the last result.\n\n" +
                "Tips: big, slow wrist movements. Hold the phone the same way you did when you taught the symbol. " +
                "Turn the switch OFF while you use Practice."
        )

        add(
            root, "Play Protect blocks the install",
            "Play Store, profile icon, Play Protect, gear icon, turn off \"Scan apps with Play Protect\". " +
                "Install AirDraw, then turn it back on."
        )

        add(
            root, "Accessibility switch is greyed out",
            "It says \"Restricted setting\". Go to Settings, Apps, AirDraw, tap the 3-dot menu, " +
                "tap \"Allow restricted settings\". Then enable the service."
        )

        add(
            root, "After an update",
            "Turn the Accessibility service OFF and ON again."
        )

        add(
            root, "\"App not installed\" when updating",
            "Debug builds change signature. Uninstall the old AirDraw first, then install the new one."
        )

        add(
            root, "Permission popups do not show",
            "If any app draws over the screen (like a floating bubble), Android blocks permission popups. " +
                "Stop that app first, or give the permission by hand in Settings, Apps, AirDraw, Permissions."
        )

        add(
            root, "Service stops by itself",
            "Some phones kill background apps. In Settings, tap \"Battery settings\" inside AirDraw " +
                "and set AirDraw to Unrestricted."
        )

        add(
            root, "Honest limits",
            "- Some phones stop sensors when the screen is off. Test it.\n" +
                "- Opening apps from the background can be blocked by Android 10+. " +
                "Turning on the Accessibility service usually fixes it.\n" +
                "- Gyroscope drift means shapes vary a little each time.\n" +
                "- Accessibility apps can be flagged by Play Protect."
        )

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor(Ui.BG))
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun add(root: LinearLayout, title: String, body: String) {
        root.addView(Ui.text(this, title, 17f, Ui.CYAN, false), Ui.params(this, Ui.WRAP, 18))
        root.addView(Ui.text(this, body, 14f, Ui.GRAY, false), Ui.params(this, Ui.WRAP, 4))
    }
}