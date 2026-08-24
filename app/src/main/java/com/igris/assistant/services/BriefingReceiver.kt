package com.igris.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.igris.assistant.data.ReminderStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val date = SimpleDateFormat("EEEE, d MMMM", Locale.US).format(Date())
        val reminders = ReminderStore(context).upcoming(System.currentTimeMillis()).take(5)
        val lines = buildString {
            append("Good morning! Today is $date.\n")
            if (reminders.isEmpty()) append("No upcoming reminders — clear runway.")
            else {
                append("Upcoming reminders:\n")
                reminders.forEach {
                    append("• ").append(SimpleDateFormat("hh:mm a", Locale.US).format(Date(it.fireAt)))
                        .append(" — ").append(it.text).append("\n")
                }
            }
        }
        NotificationHelper.post(context, NotificationHelper.CHANNEL_BRIEFING, "IGRIS Daily Briefing", lines.trim())
    }
}
