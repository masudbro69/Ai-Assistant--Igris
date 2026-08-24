package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class VaultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vault = (application as IgrisApp).locator.vault
        val ui = PanelUi(this)
        ui.install()
        ui.title("Knowledge Vault")
        ui.text("Add documents; IGRIS answers from them fully offline via Local RAG (spec §10-11).", dim = true)

        val name = ui.editText("Document name (e.g. perfume_pricing.txt)")
        val content = ui.editText("Paste document text here")
        ui.button("Add to vault") {
            val n = name.text.toString().trim()
            val c = content.text.toString()
            if (n.isNotBlank() && c.isNotBlank()) { vault.add(n, c); recreate() }
        }
        ui.divider()

        val q = ui.editText("Ask your documents a question")
        ui.button("Answer (offline RAG)") {
            val a = vault.answer(q.text.toString()) ?: "No matching document."
            ui.text(a)
        }
        ui.divider()

        ui.text("Documents (${vault.list().size}):", dim = true)
        vault.list().forEach { f ->
            ui.text("• ${f.name} (${f.length()} bytes)")
            ui.button("Delete ${f.name}") { vault.delete(f.name); recreate() }
        }
    }
}
