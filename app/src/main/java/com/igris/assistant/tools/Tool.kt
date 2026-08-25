package com.igris.assistant.tools

import android.content.Context
import com.igris.assistant.ServiceLocator
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.policy.RiskLevel

/** Everything a tool can touch, plus the confirmation state of the current action. */
class ToolContext(
    val android: Context,
    val loc: ServiceLocator,
    val confirmed: Boolean = false,
)

interface Tool {
    val id: String
    val name: String
    val description: String
    val risk: RiskLevel get() = RiskLevel.LOW
    val offlineCapable: Boolean get() = true
    fun matches(input: UnderstoodInput): Boolean
    suspend fun execute(input: UnderstoodInput, ctx: ToolContext): ToolResult
}

class ToolRegistry {
    private val tools = mutableListOf<Tool>()
    fun register(t: Tool): ToolRegistry { tools += t; return this }
    fun all(): List<Tool> = tools.toList()
    fun byId(id: String): Tool? = tools.firstOrNull { it.id == id }
    fun firstMatch(input: UnderstoodInput): Tool? = tools.firstOrNull { it.matches(input) }
}
