package com.igris.assistant.brain

import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.util.LanguageDetector
import com.igris.assistant.util.Texts
import com.igris.assistant.util.ToneAnalyzer

/**
 * Offline task classifier — the first stage of the IGRIS Brain (spec §3, §4).
 * Uses Bangla + English keyword rules so it works with zero network.
 */
object TaskClassifier {

    fun understand(raw: String): UnderstoodInput {
        val lower = raw.lowercase()
        return UnderstoodInput(
            raw = raw,
            language = LanguageDetector.detect(raw),
            style = ToneAnalyzer.style(raw),
            task = classify(lower),
            numbers = Texts.extractNumbers(raw),
            keywords = Texts.keywords(raw),
        )
    }

    private fun classify(t: String): TaskClass {
        fun has(vararg keys: String) = keys.any { t.contains(it) }
        return when {
            has("good morning", "shubho shokal", "briefing", "sokal") && !has("timer") -> TaskClass.BRIEFING
            isCalc(t) -> TaskClass.CALCULATION
            has("flash", "torch", "batir", "বাতি") -> TaskClass.DEVICE_CONTROL
            has("volume", "shobdo", "আওয়াজ", "mute") -> TaskClass.DEVICE_CONTROL
            has("open ", "launch ", "chalu koro", "খোলো") -> TaskClass.DEVICE_CONTROL
            has("battery", "storage", "ram", "device info", "diagnostics") -> TaskClass.SYSTEM_INFO
            has("timer", "countdown") -> TaskClass.TIMER
            has("alarm") -> TaskClass.ALARM
            has("remind", "reminder", "mone koriye") -> TaskClass.REMINDER
            has("translate", "anubad", "ortho", "meaning") -> TaskClass.TRANSLATION
            has("note") -> TaskClass.NOTE
            has("journal", "daily summary") -> TaskClass.JOURNAL
            has("study", "flashcard", "quiz", "exam") -> TaskClass.STUDY
            has("remember", "forget", "mone rakho", "মনে রাখো", "memory") -> TaskClass.MEMORY
            has("project") -> TaskClass.PROJECT
            has("routine") -> TaskClass.ROUTINE
            has("privacy", "wipe", "delete my data", "delete all") -> TaskClass.PRIVACY
            has("history", "log") -> TaskClass.SYSTEM_INFO
            has("notification", "notif") -> TaskClass.NOTIFICATIONS
            has("knowledge", "vault", "document", "pricing", "pdf") -> TaskClass.KNOWLEDGE
            has("find my", "file", "presentation") -> TaskClass.FILE_SEARCH
            has("search", "everything", "khuj") -> TaskClass.SMART_SEARCH
            has("plan", "help me", "youtube channel", "steps", "business") -> TaskClass.PLANNING
            has("camera", "photo", "image", "chobi", "ছবি", "ocr") -> TaskClass.VISION
            else -> TaskClass.CONVERSATION
        }
    }

    private fun isCalc(t: String): Boolean {
        if (t.contains("calculate") || t.contains("hisab") || t.contains("=")) return true
        val expr = com.igris.assistant.util.ExpressionEvaluator.extract(t) ?: return false
        return expr.count { "+-*/^%".contains(it) } >= 1 && expr.any { it.isDigit() }
    }
}
