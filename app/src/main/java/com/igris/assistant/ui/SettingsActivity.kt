package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = (application as IgrisApp).locator.settings
        val ui = PanelUi(this)
        ui.install()
        ui.title("IGRIS Settings")

        ui.text("PERSONALITY (spec §22)", dim = true)
        val name = ui.editText("Assistant name").also { it.setText(s.personalityName) }
        val style = ui.editText("Style (Professional / Friendly / Teacher)").also { it.setText(s.personalityStyle) }
        val len = ui.editText("Response length (Short / Long)").also { it.setText(s.personalityLength) }

        ui.divider()
        ui.text("MODES & PRIVACY (spec §14, §45)", dim = true)
        ui.toggle("Offline Fortress (block all online AI)", s.offlineFortress) { s.offlineFortress = it }
        ui.toggle("Proactive suggestions", s.proactiveSuggestions) { s.proactiveSuggestions = it }
        ui.toggle("Voice input", s.voiceEnabled) { s.voiceEnabled = it }
        ui.toggle("Wake word “IGRIS” — say IGRIS, get the popup anywhere", s.wakeWordEnabled) {
            s.wakeWordEnabled = it
            if (it) com.igris.assistant.services.WakeControl.start(this)
            else com.igris.assistant.services.WakeControl.stop(this)
        }
        ui.toggle("Speak responses", s.speakResponses) { s.speakResponses = it }
        ui.toggle("Daily morning briefing", s.dailyBriefing) { s.dailyBriefing = it }
        ui.toggle("Notification summary", s.notificationSummary) { s.notificationSummary = it }

        ui.text("Voice style: deep male, low pitch, calm pace (realistic, not robotic).", dim = true)

        ui.divider()
        ui.text("FREE AI — OpenCode Zen (spec §4)", dim = true)
        ui.text("Zero-cost online intelligence: IGRIS rotates across free models (Big Pickle, MiniMax M2.5 Free, Nemotron 3 Super Free, MiMo V2 Pro/Flash Free, DeepSeek V4 Flash Free, GPT-5 Nano). Offline rule engine still works with no internet.", dim = true)
        ui.toggle("Use free OpenCode Zen models when online", s.cloudProvider == "zen") {
            s.cloudProvider = if (it) "zen" else "none"
        }
        val key = ui.editText("Zen API key (optional — free tier works without billing)").also { it.setText(s.cloudApiKey) }
        val model = ui.editText("Preferred free model").also { it.setText(s.cloudModel) }
        listOf("big-pickle", "minimax-m2.5-free", "nemotron-3-super-free", "mimo-v2-pro-free", "deepseek-v4-flash-free", "gpt-5-nano").forEach { m ->
            ui.button(m) { model.setText(m) }
        }

        ui.divider()
        ui.text("ADVANCED — custom endpoint", dim = true)
        val provider = ui.editText("Provider: zen / openai / none").also { it.setText(s.cloudProvider) }
        val baseUrl = ui.editText("Base URL (blank = Zen default)").also { it.setText(s.cloudBaseUrl) }
        val ollama = ui.editText("Ollama host").also { it.setText(s.ollamaHost) }
        ui.button("Save") {
            s.personalityName = name.text.toString().ifBlank { "IGRIS" }
            s.personalityStyle = style.text.toString().ifBlank { "Professional" }
            s.personalityLength = len.text.toString().ifBlank { "Short" }
            s.cloudProvider = provider.text.toString().ifBlank { "zen" }
            s.cloudBaseUrl = baseUrl.text.toString()
            s.cloudModel = model.text.toString().ifBlank { "big-pickle" }
            s.cloudApiKey = key.text.toString()
            s.ollamaHost = ollama.text.toString()
            finish()
        }
        ui.button("Reset all settings") { s.resetAll(); finish() }
    }
}
