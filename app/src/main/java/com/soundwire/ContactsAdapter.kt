package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ContactsAdapter(
    private val users: List<User>,
    private val onChat: (User) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnChat: Button = v.findViewById(R.id.btnChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        holder.tvName.text = u.username
        holder.tvStatus.text = u.status ?: ""

        val avatar = u.avatarUrl
        if (!avatar.isNullOrBlank()) {
            Glide.with(holder.ivAvatar.context)
                .load(avatar)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.btnChat.setOnClickListener { onChat(u) }
        holder.itemView.setOnClickListener { onChat(u) }
    }

    override fun getItemCount(): Int = users.size
}
