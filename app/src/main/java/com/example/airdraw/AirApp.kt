package com.example.airdraw

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter

class AirApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val report = "Thread: " + thread.name +
                    "\nAndroid " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")" +
                    "\nPhone: " + Build.MANUFACTURER + " " + Build.MODEL +
                    "\n\n" + sw.toString()
                getSharedPreferences("crash", Context.MODE_PRIVATE)
                    .edit()
                    .putString("trace", report.take(4000))
                    .commit()
            } catch (e: Throwable) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}