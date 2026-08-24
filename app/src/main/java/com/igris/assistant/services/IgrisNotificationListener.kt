package com.igris.assistant.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Smart Notification Summary (spec §28). Only used when the user explicitly enables
 * notification access; buffers recent posts locally for summarization.
 */
class IgrisNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val title = sbn.notification?.extras?.getCharSequence("android.title")?.toString() ?: ""
        val text = sbn.notification?.extras?.getCharSequence("android.text")?.toString() ?: ""
        synchronized(buffer) {
            buffer.add(Entry(System.currentTimeMillis(), pkg, title, text))
            while (buffer.size > 100) buffer.removeAt(0)
        }
    }

    data class Entry(val at: Long, val pkg: String, val title: String, val text: String)

    companion object {
        private val buffer = mutableListOf<Entry>()
        fun recent(): List<Entry> = synchronized(buffer) { buffer.toList() }
    }
}
