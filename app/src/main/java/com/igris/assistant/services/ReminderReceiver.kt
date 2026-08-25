package com.igris.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.igris.assistant.data.ReminderStore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("reminder_id", -1)
        val text = intent.getStringExtra("reminder_text") ?: "Reminder"
        NotificationHelper.post(context, NotificationHelper.CHANNEL_ACTIONS, "IGRIS Reminder", text)
        if (id >= 0) ReminderStore(context).markDone(id)
    }
}
