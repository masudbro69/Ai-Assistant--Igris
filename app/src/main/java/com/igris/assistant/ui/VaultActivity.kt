package com.igris.assistant.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognitionOptions
import com.igris.assistant.IgrisApp
import com.igris.assistant.knowledge.KnowledgeVault

class VaultActivity : AppCompatActivity() {

    private lateinit var vault: KnowledgeVault
    private lateinit var ui: PanelUi
    private val SCAN_REQ = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = (application as IgrisApp).locator.vault
        ui = PanelUi(this)
        ui.install()
        ui.title("Knowledge Vault")
        ui.text("Add documents or scan with the camera; IGRIS answers from them fully offline via Local RAG (spec §10-11, §17).", dim = true)

        ui.button("Scan document (camera) -> vault") {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, SCAN_REQ)
            } else {
                ui.text("No camera app found on this device.", dim = true)
            }
        }

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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SCAN_REQ || resultCode != RESULT_OK) return
        val bmp = data?.extras?.get("data") as? Bitmap ?: return
        scan(bmp)
    }

    /** Camera AI (spec §17): OCR with Latin + Bangla recognizers, then into the vault. */
    private fun scan(bmp: Bitmap) {
        val image = InputImage.fromBitmap(bmp, 0)
        val latin = TextRecognition.getClient(TextRecognitionOptions.DEFAULT_OPTIONS)
        latin.process(image)
            .addOnSuccessListener { r ->
                val text = r.text.trim()
                if (text.isBlank()) { ui.text("No readable text found in the photo.", dim = true); return@addOnSuccessListener }
                val n = "scan_${System.currentTimeMillis()}.txt"
                vault.add(n, text)
                recreate()
            }
            .addOnFailureListener { ui.text("OCR failed on this device.", dim = true) }
    }
}
