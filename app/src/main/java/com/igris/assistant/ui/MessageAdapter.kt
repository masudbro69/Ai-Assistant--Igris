package com.igris.assistant.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.igris.assistant.R
import com.igris.assistant.databinding.ItemMessageBinding

enum class Role { USER, BOT }
data class ChatMessage(val role: Role, val text: String, val meta: String = "")

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.VH>() {
    private val items = mutableListOf<ChatMessage>()

    fun add(m: ChatMessage) {
        items += m
        notifyItemInserted(items.size - 1)
    }

    fun last() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b = holder.b
        b.messageText.text = m.text
        b.messageMeta.text = m.meta
        val lp = b.messageText.layoutParams as LinearLayout.LayoutParams
        if (m.role == Role.USER) {
            b.messageText.setBackgroundResource(R.drawable.bubble_user)
            lp.gravity = android.view.Gravity.END
            b.messageMeta.gravity = android.view.Gravity.END
        } else {
            b.messageText.setBackgroundResource(R.drawable.bubble_bot)
            lp.gravity = android.view.Gravity.START
            b.messageMeta.gravity = android.view.Gravity.START
        }
        b.messageText.layoutParams = lp
    }

    class VH(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)
}
