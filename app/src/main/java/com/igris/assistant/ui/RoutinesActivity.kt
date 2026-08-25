package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class RoutinesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rs = (application as IgrisApp).locator.routines
        val ui = PanelUi(this)
        ui.install()
        ui.title("Routines")
        ui.button("Create morning routine") {
            rs.add("Morning routine", listOf("Wake", "Read reminders", "Read calendar", "Start selected music", "Daily briefing"))
            recreate()
        }
        ui.divider()
        if (rs.all().isEmpty()) ui.text("No routines yet.", dim = true)
        rs.all().forEach { r ->
            ui.text("■ ${r.name} ${if (r.enabled) "(enabled)" else "(disabled)"}")
            r.steps.forEach { ui.text("   • $it", dim = true) }
            ui.toggle("Enable ${r.name}", r.enabled) { rs.toggle(r.id, it) }
            ui.button("Delete ${r.name}") { rs.delete(r.id); recreate() }
            ui.divider()
        }
    }
}
