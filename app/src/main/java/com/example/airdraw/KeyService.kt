package com.example.airdraw

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyService : AccessibilityService() {

    companion object {
        @Volatile
        var connected = false
    }

    private var held = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        if (info != null) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            serviceInfo = info
        }
        connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        connected = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val s = AirService.instance ?: return false
        if (!s.volumeEnabled()) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) held = s.volumeDown()
            return held
        }
        if (event.action == KeyEvent.ACTION_UP) {
            if (held) {
                held = false
                s.volumeUp()
                return true
            }
            return false
        }
        return false
    }
}