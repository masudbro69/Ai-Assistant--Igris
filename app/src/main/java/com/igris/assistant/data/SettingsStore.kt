package com.igris.assistant.data

import android.content.Context
import android.content.SharedPreferences

/** Central, user-controllable configuration (spec §22 personality, §45 network lock, etc.). */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("igris_settings", Context.MODE_PRIVATE)

    var mode: String
        get() = prefs.getString("mode", "ASSISTANT") ?: "ASSISTANT"
        set(v) = prefs.edit().putString("mode", v).apply()

    /** Offline Fortress: hard-disables every online path. */
    var offlineFortress: Boolean
        get() = prefs.getBoolean("offline_fortress", false)
        set(v) = prefs.edit().putBoolean("offline_fortress", v).apply()

    var proactiveSuggestions: Boolean
        get() = prefs.getBoolean("proactive", true)
        set(v) = prefs.edit().putBoolean("proactive", v).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) = prefs.edit().putBoolean("voice", v).apply()

    var speakResponses: Boolean
        get() = prefs.getBoolean("speak", true)
        set(v) = prefs.edit().putBoolean("speak", v).apply()

    var preferredLanguage: String
        get() = prefs.getString("lang", "auto") ?: "auto"
        set(v) = prefs.edit().putString("lang", v).apply()

    var personalityName: String
        get() = prefs.getString("p_name", "IGRIS") ?: "IGRIS"
        set(v) = prefs.edit().putString("p_name", v).apply()

    var personalityStyle: String
        get() = prefs.getString("p_style", "Professional") ?: "Professional"
        set(v) = prefs.edit().putString("p_style", v).apply()

    var personalityLength: String
        get() = prefs.getString("p_len", "Short") ?: "Short"
        set(v) = prefs.edit().putString("p_len", v).apply()

    var cloudProvider: String
        get() = prefs.getString("cloud_provider", "none") ?: "none"
        set(v) = prefs.edit().putString("cloud_provider", v).apply()

    var cloudBaseUrl: String
        get() = prefs.getString("cloud_url", "") ?: ""
        set(v) = prefs.edit().putString("cloud_url", v).apply()

    var cloudModel: String
        get() = prefs.getString("cloud_model", "") ?: ""
        set(v) = prefs.edit().putString("cloud_model", v).apply()

    var cloudApiKey: String
        get() = keyPrefs.getString("cloud_key", "") ?: ""
        set(v) = keyPrefs.edit().putString("cloud_key", v).apply()

    var ollamaHost: String
        get() = prefs.getString("ollama_host", "http://10.0.2.2:11343") ?: "http://10.0.2.2:11343"
        set(v) = prefs.edit().putString("ollama_host", v).apply()

    var dailyBriefing: Boolean
        get() = prefs.getBoolean("briefing", true)
        set(v) = prefs.edit().putBoolean("briefing", v).apply()

    var notificationSummary: Boolean
        get() = prefs.getBoolean("notif_summary", false)
        set(v) = prefs.edit().putBoolean("notif_summary", v).apply()

    // API keys are kept in a separate, non-backed-up preference file.
    private val keyPrefs: SharedPreferences =
        context.getSharedPreferences("igris_ai_keys", Context.MODE_PRIVATE)

    fun resetAll() {
        prefs.edit().clear().apply()
        keyPrefs.edit().clear().apply()
    }
}
