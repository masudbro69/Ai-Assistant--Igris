package com.igris.assistant.knowledge

import kotlin.math.ln
import kotlin.math.sqrt

/** Minimal TF–IDF index used by the offline Local RAG pipeline (spec §11). */
class TfidfIndex {
    private val docs = mutableMapOf<String, List<String>>()   // docId -> tokens
    private val docCount get() = docs.size

    fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9\u0980-\u09FF]+")).filter { it.length > 2 }

    fun add(id: String, text: String) {
        docs[id] = tokenize(text)
    }

    fun remove(id: String) { docs.remove(id) }
    fun clear() = docs.clear()
    fun size() = docCount

    private fun df(token: String): Int = docs.values.count { it.contains(token) }

    fun score(id: String, queryTokens: List<String>): Double {
        val tokens = docs[id] ?: return 0.0
        if (tokens.isEmpty()) return 0.0
        var s = 0.0
        for (q in queryTokens) {
            val tf = tokens.count { it == q }.toDouble() / tokens.size
            if (tf == 0.0) continue
            val idf = ln(1.0 + docCount.toDouble() / (1.0 + df(q)))
            s += tf * idf
        }
        // length-normalize query overlap
        return s / sqrt(queryTokens.size.toDouble().coerceAtLeast(1.0))
    }

    fun search(query: String, limit: Int = 5): List<Pair<String, Double>> {
        val qt = tokenize(query)
        if (qt.isEmpty()) return emptyList()
        return docs.keys
            .map { it to score(it, qt) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
    }
}
