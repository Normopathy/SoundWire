package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ChatMessageAdapter(private val items: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatMessageAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.msg_text)
        val tvTime: TextView = view.findViewById(R.id.msg_time)
        val ivAvatar: ImageView = view.findViewById(R.id.msg_avatar)
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tvText.text = m.text
        holder.tvTime.text = timeFormat.format(m.timestamp.toDate())

        if (m.avatarUrl != null) {
            Glide.with(holder.ivAvatar.context).load(m.avatarUrl).into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    override fun getItemCount(): Int = items.size

    fun add(msg: ChatMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }
}