package com.igris.assistant.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.policy.RiskLevel
import com.igris.assistant.services.NotificationHelper
import com.igris.assistant.services.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object TimeParse {
    fun durationMillis(text: String): Long? {
        val lower = text.lowercase()
        val num = com.igris.assistant.util.Texts.extractNumbers(lower).firstOrNull()
        val isSec = listOf("second", "sec", "sho", "সেকেন্ড").any { lower.contains(it) }
        val isMin = listOf("minute", "min", "minit", "মিনিট").any { lower.contains(it) }
        val isHour = listOf("hour", "hr", "ghonta", "ঘণ্টা", "ঘন্টা").any { lower.contains(it) }
        val isDay = listOf("day", "din", "দিন").any { lower.contains(it) }
        return when {
            num == null -> null
            isHour -> (num * 3_600_000).toLong()
            isDay -> (num * 86_400_000).toLong()
            isSec -> (num * 1_000).toLong()
            isMin -> (num * 60_000).toLong()
            num < 120 -> (num * 60_000).toLong() // bare number -> minutes
            else -> null
        }
    }

    fun clockTime(text: String): Long? {
        val m = Regex("(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?").find(text.lowercase()) ?: return null
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        val ampm = m.groupValues[3]
        if (ampm == "pm" && h < 12) h += 12
        if (ampm == "am" && h == 12) h = 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, h); set(java.util.Calendar.MINUTE, min)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

class TimerTool : Tool {
    override val id = "timer"
    override val name = "Timer"
    override val description = "Countdown timer with notification on completion."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.TIMER

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val ms = TimeParse.durationMillis(i.raw) ?: return ToolResult(false, "Tell me how long, e.g. “5 minute timer”.")
        val label = "${ms / 60000.0} min"
        object : CountDownTimer(ms, 1000) {
            override fun onTick(millis: Long) {}
            override fun onFinish() {
                NotificationHelper.post(ctx.android, NotificationHelper.CHANNEL_ACTIONS, "IGRIS Timer done", "Your $label timer is complete.")
            }
        }.start()
        return ToolResult(true, "Timer started for $label. I'll notify you when it's done.",
            speak = "Timer started for $label.")
    }
}

class ReminderTool : Tool {
    override val id = "reminder"
    override val name = "Reminder / alarm"
    override val description = "Schedule a reminder or alarm with exact alarm API."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.REMINDER || i.task == TaskClass.ALARM

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val now = System.currentTimeMillis()
        val fireAt = TimeParse.clockTime(i.raw) ?: TimeParse.durationMillis(i.raw)?.let { now + it }
            ?: return ToolResult(false, "Give me a time, e.g. “remind me at 8:00 pm” or “in 10 minutes”.")
        val text = i.raw
            .replace(Regex("(?i)\\b(remind me|reminder|alarm|set|at|in|please|mone koriye dio)\\b"), "")
            .trim()
            .ifBlank { "Reminder" }
        val id = ctx.loc.reminders.add(text, fireAt)
        schedule(ctx.android, id, text, fireAt)
        val when_ = SimpleDateFormat("hh:mm a", Locale.US).format(Date(fireAt))
        return ToolResult(true, "Reminder set for $when_: “$text”.", speak = "Reminder set for $when_.")
    }

    companion object {
        fun schedule(context: Context, id: Long, text: String, fireAt: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminder_id", id); putExtra("reminder_text", text)
            }
            val pi = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
                else am.set(AlarmManager.RTC_WAKEUP, fireAt, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, fireAt, pi)
            }
        }
    }
}

class BriefingTool : Tool {
    override val id = "briefing"
    override val name = "Daily briefing"
    override val description = "Good-morning briefing: date, reminders, goals (spec §9)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.BRIEFING

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date())
        val rems = ctx.loc.reminders.upcoming(System.currentTimeMillis()).take(6)
        val notes = ctx.loc.notes.all().take(3)
        val lines = buildString {
            append("Good morning! $date.\n")
            if (rems.isEmpty()) append("• No upcoming reminders.\n")
            else rems.forEach {
                append("• ").append(SimpleDateFormat("hh:mm a", Locale.US).format(Date(it.fireAt)))
                    .append(" — ").append(it.text).append("\n")
            }
            if (notes.isNotEmpty()) {
                append("Recent notes:\n")
                notes.forEach { append("  - ").append(it.title).append("\n") }
            }
            append("Online: ${if (ctx.loc.router.isOnline()) "yes" else "offline mode"}")
        }
        return ToolResult(true, lines.trim(), speak = "Good morning! Here is your briefing.")
    }
}
