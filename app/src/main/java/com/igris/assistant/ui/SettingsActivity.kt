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
        ui.toggle("Speak responses", s.speakResponses) { s.speakResponses = it }
        ui.toggle("Daily morning briefing", s.dailyBriefing) { s.dailyBriefing = it }
        ui.toggle("Notification summary", s.notificationSummary) { s.notificationSummary = it }

        ui.divider()
        ui.text("MODEL ROUTER (spec §4)", dim = true)
        val provider = ui.editText("Cloud provider: none / openai").also { it.setText(s.cloudProvider) }
        val baseUrl = ui.editText("Base URL (https://api.openai.com/v1)").also { it.setText(s.cloudBaseUrl) }
        val model = ui.editText("Model (gpt-4o-mini)").also { it.setText(s.cloudModel) }
        val key = ui.editText("API key (stored locally only)").also { it.setText(s.cloudApiKey) }
        val ollama = ui.editText("Ollama host").also { it.setText(s.ollamaHost) }
        ui.button("Save") {
            s.personalityName = name.text.toString().ifBlank { "IGRIS" }
            s.personalityStyle = style.text.toString().ifBlank { "Professional" }
            s.personalityLength = len.text.toString().ifBlank { "Short" }
            s.cloudProvider = provider.text.toString().ifBlank { "none" }
            s.cloudBaseUrl = baseUrl.text.toString()
            s.cloudModel = model.text.toString()
            s.cloudApiKey = key.text.toString()
            s.ollamaHost = ollama.text.toString()
            finish()
        }
        ui.button("Reset all settings") { s.resetAll(); finish() }
    }
}
