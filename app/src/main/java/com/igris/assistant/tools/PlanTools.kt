package com.igris.assistant.tools

import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.policy.RiskLevel

class ProjectTool : Tool {
    override val id = "project"
    override val name = "Project agent"
    override val description = "Project workspaces with tasks & deadlines (spec §12)."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.PROJECT

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val raw = i.raw
        val create = raw.contains("manage") || raw.contains("create") || raw.contains("start")
        return when {
            create -> {
                val name = raw.replace(Regex("(?i)\\b(igris|please|manage|create|start|a|my|project|for me)\\b"), "").trim()
                    .ifBlank { "New project" }
                val id = ctx.loc.projects.addProject(name)
                listOf("Tasks", "Notes", "Files", "Ideas", "Deadlines", "Milestones", "Progress")
                ctx.loc.projects.addTask(id, "Define goal", null)
                ctx.loc.projects.addTask(id, "List key tasks", null)
                ToolResult(true, "Project “$name” created with starter tasks:\n• Define goal\n• List key tasks\nOpen Projects to manage it.")
            }
            else -> {
                val ps = ctx.loc.projects.projects()
                if (ps.isEmpty()) return ToolResult(true, "No projects yet. Say “IGRIS, manage my e-commerce project”.")
                val lines = buildString {
                    ps.forEach { p ->
                        val tasks = ctx.loc.projects.tasks(p.id)
                        append("■ ${p.name} (${tasks.count { it.done }}/${tasks.size} done)\n")
                        tasks.take(5).forEach { t -> append(if (t.done) "  [x] " else "  [ ] ").append(t.title).append("\n") }
                    }
                }
                ToolResult(true, lines.trim())
            }
        }
    }
}

class RoutineTool : Tool {
    override val id = "routine"
    override val name = "Routine engine"
    override val description = "Create and toggle routines (spec §27)."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.ROUTINE

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val raw = i.raw
        return when {
            raw.contains("morning") && (raw.contains("create") || raw.contains("routine")) -> {
                val steps = listOf("Wake", "Read reminders", "Read calendar", "Start selected music", "Daily briefing")
                ctx.loc.routines.add("Morning routine", steps)
                ToolResult(true, "Morning routine created:\n" + steps.joinToString("\n") { "• $it" } + "\nEach action can be toggled in Routines.")
            }
            else -> {
                val rs = ctx.loc.routines.all()
                if (rs.isEmpty()) return ToolResult(true, "No routines yet. Try “create a morning routine”.")
                ToolResult(true, rs.joinToString("\n\n") { r ->
                    "${if (r.enabled) "●" else "○"} ${r.name}\n" + r.steps.joinToString("\n") { "   • $it" }
                })
            }
        }
    }
}

class PluginTool : Tool {
    override val id = "plugins"
    override val name = "Plugin registry"
    override val description = "List loaded tools/plugins and their permissions (spec §35, §37)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) =
        i.raw.contains("plugin") || i.raw.contains("tool") && i.raw.contains("list")

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val reg = ctx.loc.registry ?: return ToolResult(false, "Registry not ready.")
        val lines = reg.all().joinToString("\n") { t ->
            "• ${t.name} [${t.id}] offline=${if (t.offlineCapable) "yes" else "no"} risk=${t.risk}"
        }
        return ToolResult(true, "Loaded tools (${reg.all().size}):\n$lines\n\nUnknown plugins get no access by default (spec §37).")
    }
}

class PrivacyTool : Tool {
    override val id = "privacy"
    override val name = "Privacy & wipe"
    override val description = "Privacy ledger + one-tap data wipe (spec §44, §47)."
    override val risk = RiskLevel.DESTRUCTIVE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.PRIVACY

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val raw = i.raw
        return when {
            raw.contains("wipe") || raw.contains("delete my data") || raw.contains("delete all") -> {
                if (!ctx.confirmed)
                    return ToolResult(true, "This will erase local memories, notes, reminders, logs and cached AI data on this device. Confirm to proceed.", confirmRisk = RiskLevel.DESTRUCTIVE)
                ctx.loc.memory.deleteAll(); ctx.loc.reminders.deleteAll(); ctx.loc.history.clear(); ctx.loc.ledger.resetToday()
                ToolResult(true, "Local data wiped. System files outside IGRIS were not touched.", speak = "Your data has been wiped.")
            }
            else -> {
                val snap = ctx.loc.ledger.snapshot()
                val lines = buildString {
                    append("TODAY'S PRIVACY LEDGER\n")
                    snap.forEach { (k, v) -> append("• ").append(k.replace('_', ' ')).append(": ").append(v).append("\n") }
                    append("• offline fortress: ").append(if (ctx.loc.settings.offlineFortress) "ON" else "OFF").append("\n")
                    append("• cloud memory: OFF (all data stays on device)")
                }
                ToolResult(true, lines)
            }
        }
    }
}

class HistoryTool : Tool {
    override val id = "history"
    override val name = "Command history"
    override val description = "Local, deletable command history (spec §34)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.SYSTEM_INFO &&
        (i.raw.contains("history") || i.raw.contains("log"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        if (i.raw.contains("clear") || i.raw.contains("delete")) { ctx.loc.history.clear(); return ToolResult(true, "History cleared.") }
        val entries = ctx.loc.history.recent(20)
        return if (entries.isEmpty()) ToolResult(true, "No history yet.")
        else ToolResult(true, entries.joinToString("\n") { "${it.at} — [${it.kind}] ${it.summary}" })
    }
}
