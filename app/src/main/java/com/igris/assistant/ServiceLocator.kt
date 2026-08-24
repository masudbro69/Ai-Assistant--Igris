package com.igris.assistant

import android.content.Context
import com.igris.assistant.ai.ModelRouter
import com.igris.assistant.core.DeviceInspector
import com.igris.assistant.data.CommandHistoryStore
import com.igris.assistant.data.NoteStore
import com.igris.assistant.data.ProjectStore
import com.igris.assistant.data.ReminderStore
import com.igris.assistant.data.RoutineStore
import com.igris.assistant.data.SettingsStore
import com.igris.assistant.knowledge.KnowledgeVault
import com.igris.assistant.memory.MemoryStore
import com.igris.assistant.policy.PrivacyLedger
import com.igris.assistant.tools.ToolRegistry

/** Simple application-wide dependency container. */
class ServiceLocator(appContext: Context) {
    /** Populated once the tool registry is built (tools reference it at run time). */
    var registry: ToolRegistry? = null
    val context: Context = appContext
    val settings = SettingsStore(appContext)
    val ledger = PrivacyLedger(appContext)
    val device = DeviceInspector(appContext)
    val memory = MemoryStore(appContext)
    val notes = NoteStore(appContext)
    val reminders = ReminderStore(appContext)
    val routines = RoutineStore(appContext)
    val projects = ProjectStore(appContext)
    val history = CommandHistoryStore(appContext)
    val vault = KnowledgeVault(appContext)
    val router = ModelRouter(appContext, settings, ledger)
}
