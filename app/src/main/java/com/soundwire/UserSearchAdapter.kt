package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class UserSearchAdapter(
    private val users: MutableList<User>,
    private val onAdd: (User) -> Unit,
    private val onChat: (User) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnAction: Button = v.findViewById(R.id.btnChat)
    }

    private var contactIds: Set<Int> = emptySet()
    private var pendingIds: Set<Int> = emptySet()
    private var onlineIds: Set<Int> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        holder.tvName.text = u.username

        val isOnline = onlineIds.contains(u.id)
        holder.tvStatus.text = when {
            isOnline -> "online"
            !u.status.isNullOrBlank() -> u.status
            else -> ""
        }

        val avatar = u.avatarUrl
        if (!avatar.isNullOrBlank()) {
            Glide.with(holder.ivAvatar.context).load(avatar).circleCrop().into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val isContact = contactIds.contains(u.id)
        val isPending = pendingIds.contains(u.id)

        holder.btnAction.isEnabled = true
        holder.btnAction.text = when {
            isContact -> "Чат"
            isPending -> "Ожидание"
            else -> "Добавить"
        }

        if (isPending) {
            holder.btnAction.isEnabled = false
        }

        holder.btnAction.setOnClickListener {
            when {
                isContact -> onChat(u)
                else -> onAdd(u)
            }
        }

        holder.itemView.setOnClickListener {
            when {
                isContact -> onChat(u)
                else -> onAdd(u)
            }
        }
    }

    override fun getItemCount(): Int = users.size

    fun setRelations(contactIds: Set<Int>, pendingIds: Set<Int>) {
        this.contactIds = contactIds
        this.pendingIds = pendingIds
        notifyDataSetChanged()
    }

    fun setOnlineIds(ids: Set<Int>) {
        this.onlineIds = ids
        notifyDataSetChanged()
    }

    fun setData(newList: List<User>) {
        users.clear()
        users.addAll(newList)
        notifyDataSetChanged()
    }
}
