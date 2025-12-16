package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ContactsAdapter(
    private val contacts: List<AppUser>,
    private val pendingRequests: List<ContactRequest>,
    private val onActionClick: (String, String) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    companion object {
        private const val TYPE_CONTACT = 0
        private const val TYPE_PENDING = 1
        private const val TYPE_SEARCH = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < contacts.size -> TYPE_CONTACT
            else -> TYPE_PENDING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CONTACT -> {
                val view = inflater.inflate(R.layout.item_contact, parent, false)
                ContactViewHolder(view)
            }
            TYPE_PENDING -> {
                val view = inflater.inflate(R.layout.item_pending_request, parent, false)
                PendingRequestViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ContactViewHolder -> {
                val contact = contacts[position]
                holder.bind(contact)
            }
            is PendingRequestViewHolder -> {
                val requestPosition = position - contacts.size
                if (requestPosition < pendingRequests.size) {
                    val request = pendingRequests[requestPosition]
                    holder.bind(request)
                }
            }
        }
    }

    override fun getItemCount(): Int = contacts.size + pendingRequests.size

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ContactViewHolder(itemView: View) : ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnChat: Button = itemView.findViewById(R.id.btnChat)

        fun bind(contact: AppUser) {
            tvName.text = contact.displayName
            tvStatus.text = contact.status

            if (!contact.photoURL.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(contact.photoURL)
                    .circleCrop()
                    .into(ivAvatar)
            }

            btnChat.setOnClickListener {
                // Открыть чат с контактом
            }
        }
    }

    inner class PendingRequestViewHolder(itemView: View) : ViewHolder(itemView) {
        private val tvRequestInfo: TextView = itemView.findViewById(R.id.tvRequestInfo)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(request: ContactRequest) {
            // Здесь нужно получить информацию о пользователе, отправившем запрос
            tvRequestInfo.text = "Запрос на добавление в контакты"

            btnAccept.setOnClickListener {
                onActionClick(request.fromUserId, "accept")
            }

            btnReject.setOnClickListener {
                onActionClick(request.fromUserId, "reject")
            }
        }
    }
}