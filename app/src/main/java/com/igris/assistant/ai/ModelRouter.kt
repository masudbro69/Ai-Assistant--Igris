package com.igris.assistant.ai

import android.content.Context
import com.igris.assistant.core.AiPrompt
import com.igris.assistant.core.AiReply
import com.igris.assistant.core.Connectivity
import com.igris.assistant.core.ModelTier
import com.igris.assistant.core.Route
import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.data.SettingsStore
import com.igris.assistant.policy.PrivacyLedger

/**
 * Multi-Model Intelligence (spec §4 / §49): chooses the best available model for the
 * task and the current device/network situation, honouring Offline Fortress.
 */
class ModelRouter(
    private val context: Context,
    private val settings: SettingsStore,
    private val ledger: PrivacyLedger,
) {
    private val ruleEngine = RuleEngineProvider()
    private val cloud = OpenAiCompatibleProvider(settings)
    private val ollama = OllamaProvider(settings)
    private val zen = OpenCodeZenProvider(settings)

    /** Tool ids that never need a language model (Fast Intent Engine, spec §7). */
    val fastIntentTasks = setOf(
        TaskClass.CALCULATION, TaskClass.DEVICE_CONTROL, TaskClass.TIMER, TaskClass.ALARM,
        TaskClass.REMINDER, TaskClass.NOTE, TaskClass.MEMORY, TaskClass.SYSTEM_INFO,
        TaskClass.FILE_SEARCH, TaskClass.KNOWLEDGE, TaskClass.SMART_SEARCH,
        TaskClass.ROUTINE, TaskClass.PRIVACY, TaskClass.NOTIFICATIONS, TaskClass.JOURNAL,
        TaskClass.PROJECT, TaskClass.BRIEFING,
    )

    fun isFortress() = settings.offlineFortress
    fun isOnline() = Connectivity.isOnline(context) && !settings.offlineFortress

    fun route(input: UnderstoodInput, tier: ModelTier): Route = when {
        fastIntentTasks.contains(input.task) -> Route.FAST_INTENT
        settings.offlineFortress -> Route.RULE_ENGINE
        !Connectivity.isOnline(context) -> Route.RULE_ENGINE
        else -> when {
            cloud.available() -> Route.CLOUD
            ollama.available() -> Route.CLOUD
            else -> Route.RULE_ENGINE
        }
    }

    suspend fun generate(input: UnderstoodInput, route: Route, extraContext: String = ""): Pair<AiReply, Route> {
        val prompt = AiPrompt(
            system = buildSystemPrompt(input),
            user = if (extraContext.isBlank()) input.raw else "Context:\n$extraContext\n\nUser request:\n${input.raw}",
            language = input.language,
            style = input.style,
        )
        if (settings.offlineFortress || !Connectivity.isOnline(context)) {
            ledger.recordLocalVoice()
            return ruleEngine.complete(prompt) to Route.RULE_ENGINE
        }
        return try {
            val provider = when {
                zen.available() -> zen          // free models first — zero cost
                cloud.available() -> cloud
                ollama.available() -> ollama
                else -> return ruleEngine.complete(prompt) to Route.RULE_ENGINE
            }
            ledger.recordOnlineRequest()
            provider.complete(prompt) to Route.CLOUD
        } catch (e: Exception) {
            // Offline recovery chain (spec §43): cloud failure falls back to rule engine.
            ledger.recordLocalVoice()
            ruleEngine.complete(prompt) to Route.RULE_ENGINE
        }
    }

    private fun buildSystemPrompt(input: UnderstoodInput): String {
        val langLine = when (input.language) {
            com.igris.assistant.util.Lang.BN -> "Reply in Bangla (Bangla script)."
            com.igris.assistant.util.Lang.MIXED -> "Reply in natural Banglish (mixed Bangla-English)."
            else -> "Reply in English."
        }
        val styleLine = when (input.style) {
            com.igris.assistant.util.ResponseStyle.CONCISE -> "Keep it to one or two sentences."
            com.igris.assistant.util.ResponseStyle.SIMPLE -> "Explain very simply, as if to a beginner."
            com.igris.assistant.util.ResponseStyle.DETAILED -> "Give a clear, structured, detailed answer."
            com.igris.assistant.util.ResponseStyle.EMPATHETIC -> "Be warm and supportive."
            else -> if (settings.personalityLength == "Short") "Keep replies concise." else "Be helpful and clear."
        }
        return "You are ${settings.personalityName}, an offline-first personal AI operating agent. " +
            "Style: ${settings.personalityStyle}. $langLine $styleLine Never claim to upload user data."
    }
}
