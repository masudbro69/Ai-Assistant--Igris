package com.igris.assistant.core

import com.igris.assistant.policy.RiskLevel
import com.igris.assistant.util.Lang
import com.igris.assistant.util.ResponseStyle

enum class TaskClass {
    CALCULATION, DEVICE_CONTROL, MEMORY, NOTE, REMINDER, TIMER, ALARM, FILE_SEARCH,
    KNOWLEDGE, TRANSLATION, CONVERSATION, PLANNING, SYSTEM_INFO, SMART_SEARCH,
    CREATIVE, STUDY, BUSINESS, ROUTINE, PRIVACY, NOTIFICATIONS, JOURNAL, PROJECT,
    BRIEFING, VISION, UNKNOWN
}

enum class ModelTier { TINY, SMALL, MEDIUM, LARGE }
enum class Mode { ASK, ASSISTANT, AGENT, AUTOMATION, PRIVATE, OFFLINE }

data class UnderstoodInput(
    val raw: String,
    val language: Lang,
    val style: ResponseStyle,
    val task: TaskClass,
    val numbers: List<Double>,
    val keywords: List<String>,
    val mode: Mode = Mode.ASSISTANT,
)

data class AiPrompt(
    val system: String,
    val user: String,
    val language: Lang,
    val style: ResponseStyle,
    val maxTokens: Int = 512,
)

data class AiReply(
    val text: String,
    val model: String,
    val online: Boolean,
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
)

enum class Route { FAST_INTENT, RULE_ENGINE, LOCAL_MODEL, CLOUD, BLOCKED_OFFLINE }

data class BrainResponse(
    val reply: String,
    val speak: String? = null,
    val route: Route = Route.RULE_ENGINE,
    val task: TaskClass = TaskClass.UNKNOWN,
    val planLines: List<String> = emptyList(),
    val actionLog: List<String> = emptyList(),
    val memorySaved: Boolean = false,
    val needsOnline: Boolean = false,
    val confirmPending: Boolean = false,
    val pendingRisk: RiskLevel? = null,
)

/** A single step in a multi-step plan produced by the Planner. */
data class PlanStep(
    val index: Int,
    val title: String,
    val detail: String,
    val toolId: String? = null,
    val requiresApproval: Boolean = false,
)

data class Plan(val goal: String, val steps: List<PlanStep>)

/** Outcome of any tool execution. */
data class ToolResult(
    val ok: Boolean,
    val reply: String,
    val speak: String? = null,
    val data: Map<String, String> = emptyMap(),
    val confirmRisk: RiskLevel? = null,
    val verified: Boolean = true,
)
