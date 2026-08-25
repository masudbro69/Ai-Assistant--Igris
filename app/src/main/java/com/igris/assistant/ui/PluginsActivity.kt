package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class PluginsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reg = (application as IgrisApp).locator.registry
        val ui = PanelUi(this)
        ui.install()
        ui.title("Plugin / Tool Registry")
        ui.text("Each tool declares its risk level and offline capability. Unknown plugins get no access by default (spec §35, §37).", dim = true)
        ui.divider()
        reg?.all()?.forEach { t ->
            ui.text("■ ${t.name}  [${t.id}]")
            ui.text("   ${t.description}", dim = true)
            ui.text("   offline=${if (t.offlineCapable) "yes" else "no"} • risk=${t.risk}", dim = true)
            ui.divider()
        }
    }
}
