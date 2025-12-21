package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class SelectableUsersAdapter(
    private val users: MutableList<User>
) : RecyclerView.Adapter<SelectableUsersAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cb: CheckBox = v.findViewById(R.id.cbSelect)
        val iv: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
    }

    private val selected = linkedSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        holder.tvName.text = u.username
        holder.tvStatus.text = u.status ?: ""

        val avatar = u.avatarUrl
        if (!avatar.isNullOrBlank()) {
            Glide.with(holder.iv.context).load(avatar).circleCrop().into(holder.iv)
        } else {
            holder.iv.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.isChecked = selected.contains(u.id)
        holder.cb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selected.add(u.id) else selected.remove(u.id)
        }

        holder.itemView.setOnClickListener {
            val now = !selected.contains(u.id)
            if (now) selected.add(u.id) else selected.remove(u.id)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = users.size

    fun setData(list: List<User>) {
        users.clear()
        users.addAll(list)
        selected.clear()
        notifyDataSetChanged()
    }

    fun selectedIds(): List<Int> = selected.toList()
}
