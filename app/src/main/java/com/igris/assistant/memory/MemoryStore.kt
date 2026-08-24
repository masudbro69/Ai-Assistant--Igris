package com.igris.assistant.memory

import android.content.ContentValues
import android.content.Context
import com.igris.assistant.data.IgrisDatabase
import org.json.JSONArray
import org.json.JSONObject

enum class MemoryCategory { PREFERENCE, PROJECT, TASK, NOTE, KNOWLEDGE, CONTEXT, CUSTOM }

data class Memory(
    val id: Long,
    val category: MemoryCategory,
    val title: String,
    val body: String,
    val sensitive: Boolean,
    val createdAt: Long,
)

/** Local, user-controlled long-term memory (spec §23, §24). Everything stays on device. */
class MemoryStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase

    fun add(category: MemoryCategory, title: String, body: String, sensitive: Boolean = false): Long {
        val cv = ContentValues().apply {
            put("category", category.name)
            put("title", title)
            put("body", body)
            put("sensitive", if (sensitive) 1 else 0)
            put("created_at", System.currentTimeMillis())
        }
        return db.insert("memories", null, cv)
    }

    fun search(query: String, limit: Int = 8): List<Memory> {
        val tokens = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }
        val out = mutableListOf<Memory>()
        db.query("memories", null, null, null, null, null, "created_at DESC").use { c ->
            val iId = c.getColumnIndexOrThrow("id"); val iCat = c.getColumnIndexOrThrow("category")
            val iT = c.getColumnIndexOrThrow("title"); val iB = c.getColumnIndexOrThrow("body")
            val iS = c.getColumnIndexOrThrow("sensitive"); val iC = c.getColumnIndexOrThrow("created_at")
            while (c.moveToNext()) {
                val m = Memory(
                    c.getLong(iId),
                    runCatching { MemoryCategory.valueOf(c.getString(iCat)) }.getOrDefault(MemoryCategory.CUSTOM),
                    c.getString(iT), c.getString(iB), c.getInt(iS) == 1, c.getLong(iC),
                )
                val hay = (m.title + " " + m.body).lowercase()
                if (tokens.isEmpty() || tokens.any { hay.contains(it) }) out += m
                if (out.size >= limit) break
            }
        }
        return out
    }

    fun all(): List<Memory> = search("", limit = 200)

    fun forget(id: Long): Boolean = db.delete("memories", "id=?", arrayOf(id.toString())) > 0

    fun edit(id: Long, body: String): Boolean {
        val cv = ContentValues().apply { put("body", body) }
        return db.update("memories", cv, "id=?", arrayOf(id.toString())) > 0
    }

    fun deleteAll(): Int = db.delete("memories", null, null)

    fun exportJson(): String {
        val arr = JSONArray()
        all().forEach {
            arr.put(JSONObject().apply {
                put("category", it.category.name); put("title", it.title); put("body", it.body)
            })
        }
        return arr.toString(2)
    }
}
