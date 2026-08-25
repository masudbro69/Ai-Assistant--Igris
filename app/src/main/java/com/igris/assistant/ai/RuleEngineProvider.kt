package com.igris.assistant.ai

import com.igris.assistant.core.AiPrompt
import com.igris.assistant.core.AiReply
import com.igris.assistant.util.Lang

/**
 * Tiny offline rule engine (spec §7 Fast Intent Engine, §43 offline recovery).
 * Always available, zero network, used when no larger model can run.
 */
class RuleEngineProvider : AiProvider {
    override val id = "rule-engine"
    override val label = "Offline Rule Engine (tiny)"
    override val online = false
    override fun available() = true

    override suspend fun complete(prompt: AiPrompt): AiReply {
        val q = prompt.user.lowercase()
        val bangla = prompt.language == Lang.BN
        val reply = when {
            containsAny(q, listOf("hello", "hi", "salam", "assalamu")) ->
                if (bangla) "আসসালামু আলাইকুম! আমি IGRIS। বলুন, কীভাবে সাহায্য করব?"
                else "Hello! I'm IGRIS, your offline-first personal AI agent. How can I help?"
            containsAny(q, listOf("who are you", "your name", "tomar nam")) ->
                if (bangla) "আমার নাম IGRIS — Intelligent General-purpose Responsive Intelligence System।"
                else "I'm IGRIS — Intelligent General-purpose Responsive Intelligence System."
            containsAny(q, listOf("time", "somoy")) ->
                "Local time: " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date())
            containsAny(q, listOf("date", "tarikh")) ->
                "Today is " + java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.US).format(java.util.Date())
            containsAny(q, listOf("thank", "dhonnobad")) ->
                if (bangla) "আপনাকে স্বাগতম! আর কিছু লাগবে?" else "You're welcome! Anything else?"
            containsAny(q, listOf("joke", "mojar")) ->
                "Why did the offline AI cross the road? To keep its data local!"
            else -> {
                val fallback = if (bangla)
                    "আমি এখন offline rule engine চালাচ্ছি, তাই বিস্তারিত উত্তর সীমিত। আপনি চাইলে Settings-এ একটি cloud model যুক্ত করতে পারেন, অথবা আমাকে device control, timer, note, reminder, calculation-এর মতো কাজ দিতে পারেন।"
                else
                    "I'm running on my offline rule engine right now, so my free-form answers are limited. You can connect a cloud model in Settings, or give me device tasks like timers, notes, reminders, calculations and searches."
                fallback + "\n\n[offline] Your request: “${prompt.user}”"
            }
        }
        return AiReply(text = reply, model = "igris-rules-v1", online = false,
            tokensIn = prompt.user.length / 4, tokensOut = reply.length / 4)
    }

    private fun containsAny(q: String, needles: List<String>) = needles.any { q.contains(it) }
}
