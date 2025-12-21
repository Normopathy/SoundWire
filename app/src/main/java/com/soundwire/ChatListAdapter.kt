package com.soundwire

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date

class ChatListAdapter(
    private val chats: List<ChatSummary>,
    private val onChatClick: (ChatSummary) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: CircleImageView = view.findViewById(R.id.ivAvatar)
        val onlineDot: View = view.findViewById(R.id.viewOnlineDot)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    private var onlineIds: Set<Int> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]

        val isPrivate = chat.type == "private"
        val title = when {
            !chat.title.isNullOrBlank() -> chat.title
            isPrivate && chat.otherUser != null -> chat.otherUser.username
            else -> "Чат #${chat.id}"
        }

        holder.tvTitle.text = title
        holder.tvSubtitle.text = chat.lastMessage ?: ""

        val timeMs = chat.lastMessageTime ?: chat.createdAt
        holder.tvTime.text = if (timeMs != null) formatTime(timeMs) else ""

        val avatarUrl = when {
            !chat.avatarUrl.isNullOrBlank() -> chat.avatarUrl
            isPrivate -> chat.otherUser?.avatarUrl
            else -> null
        }

        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(holder.ivAvatar.context).load(avatarUrl).circleCrop().into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val otherId = chat.otherUser?.id ?: -1
        holder.onlineDot.visibility = if (isPrivate && otherId > 0 && onlineIds.contains(otherId)) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chats.size

    fun setOnlineIds(ids: Set<Int>) {
        onlineIds = ids
        notifyDataSetChanged()
    }

    private fun formatTime(ms: Long): String {
        // Если сегодня — покажем часы:минуты, иначе дату
        val now = System.currentTimeMillis()
        val sameDay = DateFormat.format("yyyyMMdd", now) == DateFormat.format("yyyyMMdd", ms)
        return if (sameDay) {
            DateFormat.format("HH:mm", Date(ms)).toString()
        } else {
            DateFormat.format("dd.MM", Date(ms)).toString()
        }
    }
}
