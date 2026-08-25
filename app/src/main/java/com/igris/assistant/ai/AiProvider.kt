package com.igris.assistant.ai

import com.igris.assistant.core.AiPrompt
import com.igris.assistant.core.AiReply

interface AiProvider {
    val id: String
    val label: String
    val online: Boolean
    fun available(): Boolean
    suspend fun complete(prompt: AiPrompt): AiReply
}
