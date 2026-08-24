package com.igris.assistant.tools

import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.memory.MemoryCategory
import com.igris.assistant.policy.RiskLevel
import com.igris.assistant.util.ExpressionEvaluator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculatorTool : Tool {
    override val id = "calculator"
    override val name = "Calculator"
    override val description = "On-device arithmetic with functions (spec §5)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.CALCULATION

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val expr = ExpressionEvaluator.extract(i.raw) ?: return ToolResult(false, "No arithmetic found.")
        return try {
            val v = ExpressionEvaluator().evaluate(expr)
            val out = if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
            else String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
            ToolResult(true, "$expr = $out", speak = "Equals $out")
        } catch (e: Exception) {
            ToolResult(false, "Couldn't evaluate “$expr”: ${e.message}")
        }
    }
}

class TranslationTool : Tool {
    override val id = "translate"
    override val name = "Translation"
    override val description = "Offline dictionary translation for common words."
    override val risk = RiskLevel.NONE

    private val bnToEn = mapOf(
        "ধন্যবাদ" to "thank you", "স্বাগতম" to "welcome", "সকাল" to "morning", "রাত" to "night",
        "পানি" to "water", "খাবার" to "food", "বই" to "book", "স্কুল" to "school", "ভালো" to "good",
        "খারাপ" to "bad", "নাম" to "name", "সময়" to "time", "আজ" to "today", "কাল" to "tomorrow",
    )
    private val enToBn = bnToEn.entries.associate { (k, v) -> v to k }

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.TRANSLATION

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val body = i.raw
            .replace(Regex("(?i)\\b(translate|meaning|anubad|ortho|to english|to bangla|in english|in bangla)\\b"), "")
            .replace(Regex("[\"“”']"), "").trim()
        val direct = bnToEn[body] ?: enToBn[body.lowercase()]
        if (direct != null) return ToolResult(true, "“$body” → $direct")
        // word-by-word fallback
        val tokens = body.split(Regex("\\s+"))
        val mapped = tokens.map { bnToEn[it] ?: enToBn[it.lowercase()] ?: it }.joinToString(" ")
        if (mapped == body) return ToolResult(false,
            "Offline dictionary is limited. For full translation connect a cloud model in Settings, then ask again.")
        return ToolResult(true, "Approximate offline translation: $mapped")
    }
}

class NoteTool : Tool {
    override val id = "note"
    override val name = "Notes"
    override val description = "Create, search and list local notes (spec §5)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.NOTE

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val raw = i.raw
        val create = raw.contains("note") && (raw.contains("save") || raw.contains("create") || raw.contains("likho") || raw.contains("লিখো") || raw.contains("add"))
        val searchQ = if (raw.contains("find") || raw.contains("search") || raw.contains("khoj"))
            raw.replace(Regex("(?i)\\b(find|search|khoj|note|notes|my)\\b"), "").trim() else null
        return when {
            searchQ != null -> {
                val hits = ctx.loc.notes.search(searchQ)
                if (hits.isEmpty()) ToolResult(true, "No notes match “$searchQ”.")
                else ToolResult(true, hits.joinToString("\n\n") { "• ${it.title}\n  ${it.body}" })
            }
            create -> {
                val body = raw.replace(Regex("(?i)\\b(save|create|add|a note|note|likho|লিখো|please)\\b"), "").trim()
                if (body.isBlank()) return ToolResult(false, "What should the note say?")
                val title = body.split(" ").take(4).joinToString(" ")
                ctx.loc.notes.add(title, body)
                ctx.loc.memory.add(MemoryCategory.NOTE, title, body)
                ToolResult(true, "Note saved: “$body”.", speak = "Note saved.")
            }
            else -> {
                val all = ctx.loc.notes.all().take(5)
                if (all.isEmpty()) ToolResult(true, "You have no notes yet.")
                else ToolResult(true, "Recent notes:\n" + all.joinToString("\n") { "• ${it.title}" })
            }
        }
    }
}

class JournalTool : Tool {
    override val id = "journal"
    override val name = "AI journal"
    override val description = "Summarize today's notes and tasks (spec §25)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.JOURNAL

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val today = SimpleDateFormat("d MMMM", Locale.US).format(Date())
        val notes = ctx.loc.notes.all().take(5)
        val pending = ctx.loc.reminders.upcoming(System.currentTimeMillis()).take(5)
        val lines = buildString {
            append("AI Journal — $today\n")
            append("Notes today: ${notes.size}\n")
            notes.forEach { append("  - ").append(it.title).append("\n") }
            append("Pending: ${pending.size}\n")
            pending.forEach { append("  - ").append(it.text).append("\n") }
        }
        return ToolResult(true, lines.trim())
    }
}

class StudyTool : Tool {
    override val id = "study"
    override val name = "Study mode"
    override val description = "Explain, flashcards, study plan (spec §31)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.STUDY

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val topic = i.raw.replace(Regex("(?i)\\b(explain|flashcard|quiz|study|plan|about|amake|আমাকে)\\b"), "").trim()
        val fromVault = ctx.loc.vault.answer(topic)
        val fromMemory = ctx.loc.memory.search(topic, 2)
        val lines = buildString {
            append("Study Mode: $topic\n\n")
            if (fromVault != null) append(fromVault).append("\n\n")
            if (fromMemory.isNotEmpty()) {
                append("From your memory:\n")
                fromMemory.forEach { append("  - ").append(it.title).append(": ").append(it.body.take(120)).append("\n") }
                append("\n")
            }
            append("Flashcard ideas:\n")
            append("  Q: What is $topic?\n  A: (answer from your notes)\n")
            append("  Q: Why is $topic important?\n  A: (your reasoning)\n")
        }
        return ToolResult(true, lines.trim())
    }
}
