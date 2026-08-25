package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hist = (application as IgrisApp).locator.history
        val ui = PanelUi(this)
        ui.install()
        ui.title("Command History")
        ui.text("Stored locally on device; you can clear it anytime (spec §34).", dim = true)
        ui.button("Clear history") { hist.clear(); recreate() }
        ui.divider()
        ui.text("CONVERSATIONS (saved locally, searchable)", dim = true)
        val conv = (application as IgrisApp).locator.conversations.recent(60)
        if (conv.isEmpty()) ui.text("No conversations yet.", dim = true)
        else conv.forEach { ui.text("${it.at} [${it.kind}] ${it.summary.take(140)}") }
        ui.divider()
        ui.text("COMMANDS", dim = true)
        val entries = hist.recent(100)
        if (entries.isEmpty()) ui.text("No history yet.")
        else entries.forEach { ui.text("${it.at} — [${it.kind}] ${it.summary}") }
    }
}
