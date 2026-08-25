package com.igris.assistant.knowledge

import android.content.Context
import java.io.File

/**
 * Personal Knowledge Vault (spec §10) + offline Local RAG (spec §11).
 * Documents are ingested into chunks, embedded as TF–IDF vectors and answered
 * fully on device with no internet.
 */
class KnowledgeVault(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "vault").apply { mkdirs() }
    val index = TfidfIndex()

    init { rebuildIndex() }

    fun list(): List<File> = dir.listFiles().orEmpty().sortedBy { it.name }

    fun add(name: String, content: String): File {
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val f = File(dir, safe)
        f.writeText(content)
        ingest(f)
        return f
    }

    fun delete(name: String): Boolean {
        val f = File(dir, name)
        index.remove(name)
        return f.delete()
    }

    private fun ingest(f: File) {
        val text = f.readText()
        val chunks = chunk(text)
        // index whole doc plus chunks under composite ids
        index.add(f.name, text)
        chunks.forEachIndexed { i, c -> index.add("${f.name}#$i", c) }
    }

    private fun chunk(text: String, size: Int = 900): List<String> {
        if (text.length <= size) return listOf(text)
        return text.chunked(size)
    }

    private fun rebuildIndex() {
        index.clear()
        list().forEach { runCatching { ingest(it) } }
    }

    fun answer(question: String): String? {
        val hits = index.search(question, limit = 3)
        if (hits.isEmpty()) return null
        val best = hits.first()
        val docName = best.first.substringBefore("#")
        val snippet = docSnippet(docName, question)
        return snippet?.let { "From “$docName” (relevance ${String.format("%.2f", best.second)}):\n$it" }
    }

    private fun docSnippet(docName: String, question: String): String? {
        val f = File(dir, docName)
        if (!f.exists()) return null
        val text = f.readText()
        val qt = index.tokenize(question)
        val lines = text.split("\n")
        val bestLine = lines.maxByOrNull { line ->
            val lt = index.tokenize(line)
            lt.count { qt.contains(it) }.toDouble()
        } ?: return null
        val score = index.tokenize(bestLine).count { qt.contains(it) }
        return if (score > 0) bestLine.trim().take(600) else text.take(400)
    }
}
