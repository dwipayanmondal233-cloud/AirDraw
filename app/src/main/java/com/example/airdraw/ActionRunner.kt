package com.example.airdraw

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.KeyEvent

object ActionRunner {

    private var torchOn = false
    private var registered = false

    fun init(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            ensureTorchCallback(cm)
        } catch (e: Exception) {
        }
    }

    private fun ensureTorchCallback(cm: CameraManager) {
        if (registered) return
        registered = true
        cm.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                torchOn = enabled
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun toggleTorch(c: Context): String {
        val cm = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        ensureTorchCallback(cm)
        var id: String? = null
        for (i in cm.cameraIdList) {
            val ch = cm.getCameraCharacteristics(i)
            val flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val back = ch.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
            if (flash && back) {
                id = i
                break
            }
        }
        if (id == null) return "No flashlight found."
        torchOn = !torchOn
        cm.setTorchMode(id, torchOn)
        return if (torchOn) "Flashlight on" else "Flashlight off"
    }

    fun run(c: Context, a: ActionDef): String {
        try {
            when (a.type) {
                "flashlight" -> return toggleTorch(c)

                "app" -> {
                    val i = c.packageManager.getLaunchIntentForPackage(a.data)
                        ?: return "App not found."
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    c.startActivity(i)
                    return "Done: " + a.label
                }

                "camera" -> {
                    val i = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    c.startActivity(i)
                    return "Done: " + a.label
                }

                "call" -> {
                    if (c.checkSelfPermission(Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        return "Call permission is missing."
                    }
                    val i = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", a.data, null))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    c.startActivity(i)
                    return "Done: " + a.label
                }

                "url" -> {
                    var u = a.data.trim()
                    if (!u.contains("://")) u = "https://" + u
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    c.startActivity(i)
                    return "Done: " + a.label
                }

                "media" -> {
                    val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    )
                    am.dispatchMediaKeyEvent(
                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    )
                    return "Done: " + a.label
                }

                else -> return "No action set."
            }
        } catch (e: Exception) {
            return "Action failed: " + e.message
        }
    }
}