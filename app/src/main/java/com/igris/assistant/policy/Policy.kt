package com.igris.assistant.policy

enum class RiskLevel { NONE, LOW, MEDIUM, HIGH, DESTRUCTIVE }

data class ActionPolicy(
    val risk: RiskLevel,
    val requiresConfirmation: Boolean,
    val requiredPermissions: List<String>,
    val reason: String,
)

/**
 * Every agent action passes through: risk assessment → permission check → optional
 * confirmation. High-risk or destructive actions always require an explicit user "yes".
 */
object PermissionEngine {
    fun evaluate(risk: RiskLevel, requiredPermissions: List<String> = emptyList(), reason: String = ""): ActionPolicy =
        ActionPolicy(
            risk = risk,
            requiresConfirmation = risk >= RiskLevel.HIGH,
            requiredPermissions = requiredPermissions,
            reason = reason,
        )
}

/** Counts what happened locally vs online (Privacy Dashboard, spec §44). */
class PrivacyLedger(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("igris_privacy", android.content.Context.MODE_PRIVATE)

    @Synchronized fun recordLocalVoice() = bump("voice_local")
    @Synchronized fun recordOnlineRequest() = bump("online_requests")
    @Synchronized fun recordFileUpload() = bump("files_uploaded")
    @Synchronized fun recordMemoryLocal() = bump("memory_local")

    private fun bump(key: String) {
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + 1).apply()
    }

    fun snapshot(): Map<String, String> = linkedMapOf(
        "voice_processed_locally" to (prefs.getLong("voice_local", 0)).toString(),
        "online_ai_requests" to (prefs.getLong("online_requests", 0)).toString(),
        "files_uploaded" to (prefs.getLong("files_uploaded", 0)).toString(),
        "memory_entries_local" to (prefs.getLong("memory_local", 0)).toString(),
    )

    fun resetToday() = prefs.edit().clear().apply()
}
