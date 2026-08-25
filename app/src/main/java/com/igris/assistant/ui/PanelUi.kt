package com.igris.assistant.ui

import android.app.Activity
import android.graphics.Typeface
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.igris.assistant.R

/** Tiny programmatic UI builder for the secondary panels (no XML needed). */
class PanelUi(private val activity: Activity) {
    private val scroll = ScrollView(activity)
    private val column = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 32, 32, 48)
    }

    init {
        scroll.addView(column)
        scroll.setBackgroundColor(ContextCompat.getColor(activity, R.color.igris_bg))
    }

    fun install(): ScrollView {
        activity.setContentView(scroll)
        return scroll
    }

    fun title(t: String) {
        val tv = TextView(activity).apply {
            text = t
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(activity, R.color.igris_primary))
            setPadding(0, 0, 0, 24)
        }
        column.addView(tv)
    }

    fun text(t: String, dim: Boolean = false, size: Float = 15f) {
        val tv = TextView(activity).apply {
            text = t
            textSize = size
            setTextColor(ContextCompat.getColor(activity, if (dim) R.color.igris_text_dim else R.color.igris_text))
            setPadding(0, 0, 0, 16)
        }
        column.addView(tv)
    }

    fun button(label: String, onClick: () -> Unit) {
        val b = Button(activity).apply {
            text = label
            setOnClickListener { onClick() }
        }
        column.addView(b)
    }

    fun divider() {
        val v = android.view.View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                bottomMargin = 24
            }
            setBackgroundColor(ContextCompat.getColor(activity, R.color.igris_surface_alt))
        }
        column.addView(v)
    }

    fun editText(hint: String): android.widget.EditText {
        val et = android.widget.EditText(activity).apply {
            this.hint = hint
            setTextColor(ContextCompat.getColor(activity, R.color.igris_text))
            setHintTextColor(ContextCompat.getColor(activity, R.color.igris_text_dim))
            setBackgroundColor(ContextCompat.getColor(activity, R.color.igris_surface_alt))
            setPadding(20, 16, 20, 16)
        }
        column.addView(et)
        return et
    }

    fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit): android.widget.Switch {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 12)
        }
        val tv = TextView(activity).apply {
            text = label
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(ContextCompat.getColor(activity, R.color.igris_text))
        }
        val sw = android.widget.Switch(activity).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, c -> onChange(c) }
        }
        row.addView(tv); row.addView(sw)
        column.addView(row)
        return sw
    }
}
