package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

sealed class RequestItem {
    data class Header(val title: String) : RequestItem()
    data class Incoming(val request: ContactRequest) : RequestItem()
    data class Outgoing(val request: ContactRequest) : RequestItem()
}

class RequestsAdapter(
    private val items: MutableList<RequestItem>,
    private val onAccept: (Int) -> Unit,
    private val onDecline: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_INCOMING = 1
        const val TYPE_OUTGOING = 2
    }

    class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvHeader)
    }

    class IncomingVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val btnAccept: Button = v.findViewById(R.id.btnAccept)
        val btnDecline: Button = v.findViewById(R.id.btnDecline)
    }

    class OutgoingVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RequestItem.Header -> TYPE_HEADER
            is RequestItem.Incoming -> TYPE_INCOMING
            is RequestItem.Outgoing -> TYPE_OUTGOING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(inflater.inflate(R.layout.item_request_header, parent, false))
            TYPE_INCOMING -> IncomingVH(inflater.inflate(R.layout.item_request_incoming, parent, false))
            else -> OutgoingVH(inflater.inflate(R.layout.item_request_outgoing, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val it = items[position]) {
            is RequestItem.Header -> {
                (holder as HeaderVH).tv.text = it.title
            }
            is RequestItem.Incoming -> {
                val h = holder as IncomingVH
                val u = it.request.fromUser
                h.tvName.text = u?.username ?: "Пользователь"
                h.tvStatus.text = u?.status ?: ""

                val avatar = u?.avatarUrl
                if (!avatar.isNullOrBlank()) {
                    Glide.with(h.ivAvatar.context).load(avatar).circleCrop().into(h.ivAvatar)
                } else {
                    h.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                h.btnAccept.setOnClickListener { onAccept(it.request.id) }
                h.btnDecline.setOnClickListener { onDecline(it.request.id) }
            }
            is RequestItem.Outgoing -> {
                val h = holder as OutgoingVH
                val u = it.request.toUser
                h.tvName.text = u?.username ?: "Пользователь"
                h.tvStatus.text = "Ожидает подтверждения"

                val avatar = u?.avatarUrl
                if (!avatar.isNullOrBlank()) {
                    Glide.with(h.ivAvatar.context).load(avatar).circleCrop().into(h.ivAvatar)
                } else {
                    h.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setData(newItems: List<RequestItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
