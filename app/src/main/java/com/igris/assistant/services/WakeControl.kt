package com.igris.assistant.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Starts/stops the wake-word popup, guiding the user through the two required grants. */
object WakeControl {

    fun overlayGranted(c: Context): Boolean = Settings.canDrawOverlays(c)
    fun micGranted(c: Context): Boolean =
        ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Returns false and opens the relevant settings screen if a grant is missing. */
    fun start(c: Context): Boolean {
        if (!overlayGranted(c)) {
            runCatching {
                c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return false
        }
        if (!micGranted(c)) return false
        c.startService(Intent(c, WakeWordService::class.java))
        return true
    }

    fun stop(c: Context) {
        c.startService(Intent(c, WakeWordService::class.java).setAction(WakeWordService.ACTION_STOP))
    }
}
