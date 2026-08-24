package com.igris.assistant.ai

import com.igris.assistant.core.AiPrompt
import com.igris.assistant.core.AiReply
import com.igris.assistant.data.SettingsStore
import org.json.JSONArray
import org.json.JSONObject

/** Any OpenAI-compatible endpoint (OpenAI, Groq, OpenRouter, LM Studio…). */
class OpenAiCompatibleProvider(private val settings: SettingsStore) : AiProvider {
    override val id = "openai-compatible"
    override val label = "Cloud (OpenAI-compatible)"
    override val online = true

    override fun available(): Boolean {
        val s = settings
        return s.cloudProvider == "openai" && s.cloudBaseUrl.isNotBlank() && s.cloudApiKey.isNotBlank()
    }

    override suspend fun complete(prompt: AiPrompt): AiReply {
        val body = JSONObject().apply {
            put("model", settings.cloudModel.ifBlank { "gpt-4o-mini" })
            put("max_tokens", prompt.maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", prompt.system) })
                put(JSONObject().apply { put("role", "user"); put("content", prompt.user) })
            })
        }
        val headers = mapOf("Authorization" to "Bearer ${settings.cloudApiKey}")
        val res = HttpJson.post("${settings.cloudBaseUrl.trimEnd('/')}/chat/completions", headers, body)
        val text = res.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content") ?: ""
        val usage = res.optJSONObject("usage")
        return AiReply(
            text = text,
            model = res.optString("model", settings.cloudModel),
            online = true,
            tokensIn = usage?.optInt("prompt_tokens", 0) ?: 0,
            tokensOut = usage?.optInt("completion_tokens", 0) ?: 0,
        )
    }
}

/** Ollama on the device's host (e.g. adb reverse tcp:11343) or LAN. */
class OllamaProvider(private val settings: SettingsStore) : AiProvider {
    override val id = "ollama"
    override val label = "Local/LAN Ollama"
    override val online = true // requires a reachable host, but no public cloud

    override fun available(): Boolean = settings.ollamaHost.isNotBlank()

    override suspend fun complete(prompt: AiPrompt): AiReply {
        val body = JSONObject().apply {
            put("model", "llama3")
            put("stream", false)
            put("prompt", "${prompt.system}\n\nUser: ${prompt.user}\nAssistant:")
        }
        val res = HttpJson.post("${settings.ollamaHost.trimEnd('/')}/api/generate", emptyMap(), body)
        return AiReply(text = res.optString("response", ""), model = "ollama", online = true)
    }
}
