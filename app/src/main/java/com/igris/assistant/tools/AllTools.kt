package com.igris.assistant.tools

import com.igris.assistant.ServiceLocator

object AllTools {
    fun registry(loc: ServiceLocator): ToolRegistry {
        val reg = ToolRegistry()
            .register(FlashlightTool())
            .register(VolumeTool())
            .register(AppLaunchTool())
            .register(DeviceInfoTool())
            .register(CalculatorTool())
            .register(TimerTool())
            .register(ReminderTool())
            .register(BriefingTool())
            .register(TranslationTool())
            .register(NoteTool())
            .register(JournalTool())
            .register(StudyTool())
            .register(MemoryTool())
            .register(VaultTool())
            .register(FileSearchTool())
            .register(SmartSearchTool())
            .register(NotificationSummaryTool())
            .register(ProjectTool())
            .register(RoutineTool())
            .register(PluginTool())
            .register(HistoryTool())
            .register(PrivacyTool())
        loc.registry = reg
        return reg
    }
}
