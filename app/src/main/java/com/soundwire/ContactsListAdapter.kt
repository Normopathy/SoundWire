package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ContactsListAdapter(
    private val items: List<User>,
    private val onChat: (User) -> Unit,
    private val onRemove: (User) -> Unit
) : RecyclerView.Adapter<ContactsListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnRemove: Button = v.findViewById(R.id.btnRemove)
        val onlineDot: View = v.findViewById(R.id.viewOnlineDot)
    }

    private var onlineIds: Set<Int> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        holder.tvName.text = u.username

        val isOnline = onlineIds.contains(u.id)
        holder.onlineDot.visibility = if (isOnline) View.VISIBLE else View.GONE

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

        holder.itemView.setOnClickListener { onChat(u) }
        holder.btnRemove.setOnClickListener { onRemove(u) }
    }

    override fun getItemCount(): Int = items.size

    fun setOnlineIds(ids: Set<Int>) {
        onlineIds = ids
        notifyDataSetChanged()
    }
}
