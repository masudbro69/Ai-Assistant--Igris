package com.igris.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.igris.assistant.IgrisApp

class ProjectsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ps = (application as IgrisApp).locator.projects
        val ui = PanelUi(this)
        ui.install()
        ui.title("Projects")

        val name = ui.editText("New project name")
        ui.button("Create project") {
            val n = name.text.toString().trim()
            if (n.isNotBlank()) { ps.addProject(n); recreate() }
        }
        ui.divider()

        if (ps.projects().isEmpty()) ui.text("No projects yet. Say “IGRIS, manage my e-commerce project”.", dim = true)
        ps.projects().forEach { p ->
            ui.text("■ ${p.name}")
            val tasks = ps.tasks(p.id)
            ui.text("   ${tasks.count { it.done }}/${tasks.size} tasks done", dim = true)
            tasks.forEach { t ->
                ui.toggle((if (t.done) "[x] " else "[ ] ") + t.title, t.done) { ps.toggleTask(t.id, it) }
            }
            val tName = ui.editText("Add task to ${p.name}")
            ui.button("Add task") {
                val v = tName.text.toString().trim()
                if (v.isNotBlank()) { ps.addTask(p.id, v); recreate() }
            }
            ui.divider()
        }
    }
}
