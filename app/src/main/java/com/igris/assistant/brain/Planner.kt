package com.igris.assistant.brain

import com.igris.assistant.core.Plan
import com.igris.assistant.core.PlanStep
import com.igris.assistant.core.UnderstoodInput

/**
 * Task Planner (spec §13): decomposes complex goals into ordered, approval-aware steps.
 * Templates run offline; a connected model can refine them.
 */
object Planner {

    fun plan(input: UnderstoodInput): Plan {
        val t = input.raw.lowercase()
        return when {
            t.contains("youtube") -> Plan("Start a YouTube channel", steps(
                "Choose a niche you can sustain",
                "Define your target audience",
                "Create branding (name, logo, banner)",
                "Build a 4-week content plan",
                "Set a production workflow (script, record, edit)",
                "Create a publishing schedule",
                "Track growth and iterate",
            ))
            t.contains("business") || t.contains("shop") -> Plan("Launch a small business", steps(
                "Define product & pricing",
                "Identify target customers",
                "Set up a simple storefront / page",
                "Plan first marketing push",
                "Track orders & payments",
                "Review weekly metrics",
            ))
            t.contains("study") || t.contains("exam") || t.contains("routine") -> Plan("Study plan", steps(
                "List subjects & syllabus",
                "Prioritize by exam date",
                "Create daily time blocks",
                "Add active-recall sessions (flashcards)",
                "Schedule weekly revision",
                "Track progress",
            ))
            else -> Plan(input.raw, steps(
                "Clarify the goal & success criteria",
                "Break into small tasks",
                "Order tasks by dependency",
                "Assign effort & deadlines",
                "Execute with confirmation on risky steps",
                "Verify & report results",
            ))
        }
    }

    private fun steps(vararg titles: String): List<PlanStep> =
        titles.mapIndexed { i, s -> PlanStep(i + 1, s, detail = s, requiresApproval = false) }
}
