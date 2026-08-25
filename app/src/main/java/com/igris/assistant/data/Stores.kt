package com.igris.assistant.data

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray

data class Note(val id: Long, val title: String, val body: String, val createdAt: Long)
data class Reminder(val id: Long, val text: String, val fireAt: Long, val done: Boolean)
data class Routine(val id: Long, val name: String, val steps: List<String>, val enabled: Boolean)
data class Project(val id: Long, val name: String, val createdAt: Long)
data class ProjectTask(val id: Long, val projectId: Long, val title: String, val done: Boolean, val deadline: String?)
data class HistoryEntry(val id: Long, val at: String, val kind: String, val summary: String)

class NoteStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun add(title: String, body: String): Long =
        db.insert("notes", null, ContentValues().apply {
            put("title", title); put("body", body); put("created_at", System.currentTimeMillis())
        })
    fun all(): List<Note> {
        val out = mutableListOf<Note>()
        db.query("notes", null, null, null, null, null, "created_at DESC").use { c ->
            while (c.moveToNext()) out += Note(
                c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)
            )
        }
        return out
    }
    fun search(q: String): List<Note> {
        val t = q.lowercase()
        return all().filter { (it.title + " " + it.body).lowercase().contains(t) }
    }
    fun delete(id: Long) = db.delete("notes", "id=?", arrayOf(id.toString()))
}

class ReminderStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun add(text: String, fireAt: Long): Long =
        db.insert("reminders", null, ContentValues().apply {
            put("text", text); put("fire_at", fireAt); put("done", 0)
        })
    fun pending(): List<Reminder> {
        val out = mutableListOf<Reminder>()
        db.query("reminders", null, "done=0", null, null, null, "fire_at ASC").use { c ->
            while (c.moveToNext()) out += Reminder(c.getLong(0), c.getString(1), c.getLong(2), false)
        }
        return out
    }
    fun upcoming(now: Long): List<Reminder> = pending().filter { it.fireAt > now }
    fun markDone(id: Long) = db.update("reminders", ContentValues().apply { put("done", 1) }, "id=?", arrayOf(id.toString()))
    fun deleteAll() = db.delete("reminders", null, null)
}

class RoutineStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun add(name: String, steps: List<String>): Long =
        db.insert("routines", null, ContentValues().apply {
            put("name", name); put("steps_json", JSONArray(steps).toString()); put("enabled", 1)
        })
    fun all(): List<Routine> {
        val out = mutableListOf<Routine>()
        db.query("routines", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) {
                val arr = runCatching { JSONArray(c.getString(2)) }.getOrNull()
                val steps = (0 until (arr?.length() ?: 0)).map { arr!!.getString(it) }
                out += Routine(c.getLong(0), c.getString(1), steps, c.getInt(3) == 1)
            }
        }
        return out
    }
    fun toggle(id: Long, enabled: Boolean) =
        db.update("routines", ContentValues().apply { put("enabled", if (enabled) 1 else 0) }, "id=?", arrayOf(id.toString()))
    fun delete(id: Long) = db.delete("routines", "id=?", arrayOf(id.toString()))
}

class ProjectStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun addProject(name: String): Long =
        db.insert("projects", null, ContentValues().apply {
            put("name", name); put("created_at", System.currentTimeMillis())
        })
    fun projects(): List<Project> {
        val out = mutableListOf<Project>()
        db.query("projects", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) out += Project(c.getLong(0), c.getString(1), c.getLong(2))
        }
        return out
    }
    fun addTask(projectId: Long, title: String, deadline: String? = null): Long =
        db.insert("project_tasks", null, ContentValues().apply {
            put("project_id", projectId); put("title", title); put("done", 0)
            if (deadline != null) put("deadline", deadline)
        })
    fun tasks(projectId: Long): List<ProjectTask> {
        val out = mutableListOf<ProjectTask>()
        db.query("project_tasks", null, "project_id=?", arrayOf(projectId.toString()), null, null, "id ASC").use { c ->
            while (c.moveToNext()) out += ProjectTask(
                c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3) == 1,
                if (c.isNull(4)) null else c.getString(4)
            )
        }
        return out
    }
    fun toggleTask(id: Long, done: Boolean) =
        db.update("project_tasks", ContentValues().apply { put("done", if (done) 1 else 0) }, "id=?", arrayOf(id.toString()))
}

class ConversationStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun log(role: String, text: String) {
        val at = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
        db.insert("conversations", null, ContentValues().apply {
            put("role", role); put("text", text.take(2000)); put("at_time", at)
        })
        db.execSQL("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY id DESC LIMIT 1000)")
    }
    fun recent(limit: Int = 60): List<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        db.query("conversations", null, null, null, null, null, "id DESC", limit.toString()).use { c ->
            while (c.moveToNext()) out += HistoryEntry(c.getLong(0), c.getString(3), c.getString(1), c.getString(2))
        }
        return out
    }
    fun search(q: String, limit: Int = 5): List<HistoryEntry> =
        recent(200).filter { it.summary.lowercase().contains(q.lowercase()) }.take(limit)
    fun clear() = db.delete("conversations", null, null)
}

class CommandHistoryStore(context: Context) {
    private val db = IgrisDatabase(context.applicationContext).writableDatabase
    fun log(kind: String, summary: String) {
        val at = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        db.insert("history", null, ContentValues().apply {
            put("at_time", at); put("kind", kind); put("summary", summary.take(160))
        })
        // keep the last 500 entries
        db.execSQL("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 500)")
    }
    fun recent(limit: Int = 100): List<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        db.query("history", null, null, null, null, null, "id DESC", limit.toString()).use { c ->
            while (c.moveToNext()) out += HistoryEntry(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
        }
        return out
    }
    fun clear() = db.delete("history", null, null)
}
