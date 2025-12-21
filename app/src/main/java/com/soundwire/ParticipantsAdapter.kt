package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ParticipantsAdapter(
    private val items: MutableList<ChatParticipant>
) : RecyclerView.Adapter<ParticipantsAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvRole: TextView = v.findViewById(R.id.tvRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.tvName.text = p.user.username
        holder.tvRole.text = p.role

        val avatar = p.user.avatarUrl
        if (!avatar.isNullOrBlank()) {
            Glide.with(holder.ivAvatar.context).load(avatar).circleCrop().into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(list: List<ChatParticipant>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
