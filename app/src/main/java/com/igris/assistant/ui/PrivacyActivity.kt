package com.igris.assistant.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loc = (application as IgrisApp).locator
        val ui = PanelUi(this)
        ui.install()
        ui.title("Privacy Dashboard")
        ui.text("Where your data goes (spec §44). Everything below stays on this device unless you enable a cloud model.", dim = true)
        loc.ledger.snapshot().forEach { (k, v) -> ui.text("• ${k.replace('_', ' ')}: $v") }
        ui.text("• offline fortress: ${if (loc.settings.offlineFortress) "ON" else "OFF"}")
        ui.text("• cloud memory: OFF", )
        ui.divider()

        ui.toggle("Offline Fortress", loc.settings.offlineFortress) { loc.settings.offlineFortress = it }
        ui.toggle("Notification summary", loc.settings.notificationSummary) { loc.settings.notificationSummary = it }

        ui.button("Open Android notification access") {
            runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        ui.button("Delete My Data (one-tap wipe)") {
            AlertDialog.Builder(this)
                .setTitle("Delete My Data?")
                .setMessage("Erases local memories, notes, reminders, logs and cached AI data on this device. System files outside IGRIS are not touched.")
                .setPositiveButton("Delete") { _, _ ->
                    loc.memory.deleteAll(); loc.reminders.deleteAll(); loc.history.clear(); loc.ledger.resetToday()
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        ui.button("Export memories (JSON)") {
            val json = loc.memory.exportJson()
            val f = java.io.File(filesDir, "igris_memory_export.json")
            f.writeText(json)
            ui.text("Exported to ${f.name}", dim = true)
        }
    }
}
