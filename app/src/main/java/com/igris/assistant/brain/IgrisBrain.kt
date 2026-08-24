package com.igris.assistant.brain

import com.igris.assistant.ServiceLocator
import com.igris.assistant.core.BrainResponse
import com.igris.assistant.core.Route
import com.igris.assistant.core.TaskClass
import com.igris.assistant.policy.PermissionEngine
import com.igris.assistant.tools.AllTools
import com.igris.assistant.tools.ToolContext as TC

/**
 * The central AI Brain (spec §3, §51):
 * Understand → Think → Plan → Permission Check → Execute → Verify → (optionally) Remember.
 */
class IgrisBrain(private val loc: ServiceLocator) {

    private val registry = AllTools.registry(loc)

    suspend fun handle(text: String, confirmed: Boolean = false): BrainResponse {
        val input = TaskClassifier.understand(text)
        val tier = loc.device.recommendedTier()
        val route = loc.router.route(input, tier)
        val actions = mutableListOf<String>()

        // ---- Fast Intent Engine: deterministic tools, no LLM (spec §7) ----
        val tool = registry.firstMatch(input)
        if (route == Route.FAST_INTENT && tool != null) {
            val policy = PermissionEngine.evaluate(tool.risk)
            if (policy.requiresConfirmation && !confirmed) {
                return BrainResponse(
                    reply = "This action is ${tool.risk.name}. ${tool.description}\n\nConfirm to proceed?",
                    confirmPending = true, pendingRisk = tool.risk, task = input.task,
                )
            }
            val res = tool.execute(input, TC(loc.context, loc, confirmed))
            actions += "[${tool.id}] ${if (res.ok) "ok" else "failed"}"
            loc.history.log(tool.id, text)
            return BrainResponse(
                reply = res.reply, speak = res.speak ?: res.reply,
                route = Route.FAST_INTENT, task = input.task, actionLog = actions,
            )
        }

        // ---- Planner: multi-step goals (spec §13) ----
        if (input.task == TaskClass.PLANNING) {
            val plan = Planner.plan(input)
            val lines = plan.steps.map { "${it.index}. ${it.title}" }
            val (reply, usedRoute) = enrich(plan.goal, input, route)
            loc.history.log("planner", text)
            return BrainResponse(
                reply = "Plan: ${plan.goal}\n${lines.joinToString("\n")}\n\n$reply",
                speak = "Here's a plan for ${plan.goal}.",
                route = usedRoute, task = input.task, planLines = lines, actionLog = actions,
            )
        }

        // ---- Language model path: rule engine offline, cloud when allowed ----
        val (ai, usedRoute) = loc.router.generate(input, route)
        loc.history.log("ai", text)
        return BrainResponse(
            reply = ai.text, speak = ai.text, route = usedRoute, task = input.task,
            needsOnline = ai.online, actionLog = actions,
        )
    }

    private suspend fun enrich(goal: String, input: com.igris.assistant.core.UnderstoodInput, route: Route): Pair<String, Route> {
        val r = loc.router.generate(input, route, "The user asked to plan: $goal. Add one practical tip.")
        return r.first.text to r.second
    }
}
