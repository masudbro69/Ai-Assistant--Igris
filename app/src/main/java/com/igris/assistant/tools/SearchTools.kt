package com.igris.assistant.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.memory.MemoryCategory
import com.igris.assistant.policy.RiskLevel
import com.igris.assistant.services.IgrisNotificationListener
import java.io.File

class MemoryTool : Tool {
    override val id = "memory"
    override val name = "Memory"
    override val description = "Remember / forget / search / export personal memory (spec §23-24)."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.MEMORY

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val raw = i.raw
        return when {
            raw.contains("don't remember") || raw.contains("dont remember") || raw.contains("mone rekho na") ->
                ToolResult(true, "Understood — I will not store that in persistent memory.", speak = "I won't remember that.")
            raw.contains("forget") || raw.contains("bhule jao") || raw.contains("ভুলে") -> {
                val q = raw.replace(Regex("(?i)\\b(forget|delete|remove|bhule jao|ভুলে যাও)\\b"), "").trim()
                val hits = ctx.loc.memory.search(q)
                if (hits.isEmpty()) return ToolResult(true, "Nothing in memory matches “$q”.")
                hits.forEach { ctx.loc.memory.forget(it.id) }
                ctx.loc.history.log("memory", "Forgot ${hits.size} entries matching $q")
                ToolResult(true, "Forgot ${hits.size} memory entries matching “$q”.")
            }
            raw.contains("export") -> {
                val json = ctx.loc.memory.exportJson()
                val f = File(ctx.android.filesDir, "igris_memory_export.json")
                f.writeText(json)
                ToolResult(true, "Memory exported to ${f.name} (${ctx.loc.memory.all().size} entries).")
            }
            raw.contains("remember") || raw.contains("mone rakho") || raw.contains("মনে রাখো") -> {
                val body = raw.replace(Regex("(?i)\\b(please|remember|that|mone rakho|মনে রাখো)\\b"), "").trim()
                if (body.isBlank()) return ToolResult(false, "What should I remember?")
                ctx.loc.memory.add(MemoryCategory.CUSTOM, body.split(" ").take(4).joinToString(" "), body)
                ctx.loc.ledger.recordMemoryLocal()
                ToolResult(true, "Remembered: “$body”.", speak = "Remembered.")
            }
            else -> {
                val hits = ctx.loc.memory.search(raw)
                if (hits.isEmpty()) ToolResult(true, "No memories match that yet.")
                else ToolResult(true, hits.joinToString("\n") { "• ${it.title}: ${it.body.take(120)}" })
            }
        }
    }
}

class VaultTool : Tool {
    override val id = "vault"
    override val name = "Knowledge vault"
    override val description = "Answer from your local documents via Local RAG (spec §10-11)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.KNOWLEDGE

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val q = i.raw.replace(Regex("(?i)\\b(what|is|in|my|document|vault|note|kotha|আছে|কী|লেখা)\\b"), "").trim()
        val answer = ctx.loc.vault.answer(i.raw) ?: ctx.loc.vault.answer(q)
        return if (answer != null) ToolResult(true, answer)
        else ToolResult(true,
            "I don't have a document matching that in your vault yet.\nAdd documents from the Vault screen, then ask me again — I'll answer fully offline.")
    }
}

class FileSearchTool : Tool {
    override val id = "file"
    override val name = "File agent"
    override val description = "Semantic-ish local file search (spec §29)."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.FILE_SEARCH

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val q = i.raw.replace(Regex("(?i)\\b(find|search|my|file|files|khoj|খোঁজো|presentation|document)\\b"), "").trim()
        val results = mutableListOf<String>()
        // 1) app-local vault files always searchable (no permission needed)
        ctx.loc.vault.list().filter { it.name.lowercase().contains(q.lowercase()) }.forEach { results += "vault/${it.name}" }
        // 2) shared storage only if the user granted access
        if (hasStorage(ctx.android)) {
            val roots = listOf(
                android.os.Environment.getExternalStorageDirectory(),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
            )
            roots.forEach { root ->
                root?.walkTopDown()?.maxDepth(3)?.filter { it.isFile && it.name.lowercase().contains(q.lowercase()) }
                    ?.take(10)?.forEach { results += it.path }
            }
        }
        return if (results.isEmpty()) ToolResult(true,
            "No file matching “$q”. ${if (!hasStorage(ctx.android)) "Grant storage access to search shared files." else ""}")
        else ToolResult(true, "Found:\n" + results.distinct().take(10).joinToString("\n"))
    }

    private fun hasStorage(c: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED
    }
}

class SmartSearchTool : Tool {
    override val id = "smart-search"
    override val name = "Smart search"
    override val description = "One search across memory, notes, vault and history (spec §33)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.SMART_SEARCH

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val q = i.raw.replace(Regex("(?i)\\b(search|find|everything|all|khoj|খুঁজো)\\b"), "").trim()
        val out = StringBuilder()
        val mem = ctx.loc.memory.search(q, 3)
        val notes = ctx.loc.notes.search(q).take(3)
        val hist = ctx.loc.history.recent(50).filter { it.summary.lowercase().contains(q.lowercase()) }.take(3)
        val vault = ctx.loc.vault.index.search(q, 3)
        val conv = ctx.loc.conversations.search(q, 3)
        if (mem.isNotEmpty()) out.append("Memory:\n").append(mem.joinToString("\n") { "  • ${it.title}" }).append("\n")
        if (notes.isNotEmpty()) out.append("Notes:\n").append(notes.joinToString("\n") { "  • ${it.title}" }).append("\n")
        if (hist.isNotEmpty()) out.append("History:\n").append(hist.joinToString("\n") { "  • ${it.at} ${it.summary}" }).append("\n")
        if (vault.isNotEmpty()) out.append("Vault:\n").append(vault.joinToString("\n") { "  • ${it.first}" }).append("\n")
        if (conv.isNotEmpty()) out.append("Conversations:\n").append(conv.joinToString("\n") { "  • [${it.kind}] ${it.summary.take(80)}" }).append("\n")
        return if (out.isEmpty()) ToolResult(true, "Nothing found for “$q”.") else ToolResult(true, out.toString().trim())
    }
}

class NotificationSummaryTool : Tool {
    override val id = "notif-summary"
    override val name = "Notification summary"
    override val description = "Summarize authorized notifications (spec §28)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.NOTIFICATIONS

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        if (!ctx.loc.settings.notificationSummary)
            return ToolResult(true, "Notification summary is OFF. Enable it in Settings → Privacy.")
        val entries = IgrisNotificationListener.recent()
        if (entries.isEmpty()) return ToolResult(true, "No notifications captured yet. Make sure notification access is enabled for IGRIS.")
        val byPkg = entries.groupBy { it.pkg }
        val lines = buildString {
            append("Notification summary (${entries.size} recent):\n")
            byPkg.entries.sortedByDescending { it.value.size }.take(8).forEach { (pkg, list) ->
                append("• ").append(pkg.substringAfterLast('.')).append(": ").append(list.size).append("\n")
            }
        }
        return ToolResult(true, lines.trim())
    }
}
