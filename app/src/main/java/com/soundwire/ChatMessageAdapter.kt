package com.soundwire

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Date

class ChatMessageAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: Int,
    private val isGroup: Boolean,
    private val onOpenUrl: (url: String, mime: String?) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TEXT_IN = 1
        const val TEXT_OUT = 2
        const val IMG_IN = 3
        const val IMG_OUT = 4
        const val AUDIO_IN = 5
        const val AUDIO_OUT = 6
        const val FILE_IN = 7
        const val FILE_OUT = 8
    }

    private val ids = HashSet<Long>()

    init {
        messages.forEach { ids.add(it.id) }
        VoiceNotePlayer.setOnStateChangedListener { _, _ ->
            // обновим кнопки play/pause
            notifyDataSetChanged()
        }
    }

    // ----- ViewHolders -----

    class TextInVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvSender: TextView = v.findViewById(R.id.tvSender)
        val tvText: TextView = v.findViewById(R.id.tvText)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class TextOutVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvText: TextView = v.findViewById(R.id.tvText)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class ImageInVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvSender: TextView = v.findViewById(R.id.tvSender)
        val ivImage: ImageView = v.findViewById(R.id.ivImage)
        val tvCaption: TextView = v.findViewById(R.id.tvCaption)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class ImageOutVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivImage: ImageView = v.findViewById(R.id.ivImage)
        val tvCaption: TextView = v.findViewById(R.id.tvCaption)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class AudioInVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvSender: TextView = v.findViewById(R.id.tvSender)
        val btnPlay: ImageButton = v.findViewById(R.id.btnPlay)
        val tvLabel: TextView = v.findViewById(R.id.tvAudioLabel)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class AudioOutVH(v: View) : RecyclerView.ViewHolder(v) {
        val btnPlay: ImageButton = v.findViewById(R.id.btnPlay)
        val tvLabel: TextView = v.findViewById(R.id.tvAudioLabel)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class FileInVH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: CircleImageView = v.findViewById(R.id.ivAvatar)
        val tvSender: TextView = v.findViewById(R.id.tvSender)
        val tvFileName: TextView = v.findViewById(R.id.tvFileName)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    class FileOutVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvFileName: TextView = v.findViewById(R.id.tvFileName)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    override fun getItemViewType(position: Int): Int {
        val m = messages[position]
        val outgoing = m.senderId == currentUserId
        return when (m.type) {
            "image" -> if (outgoing) IMG_OUT else IMG_IN
            "audio" -> if (outgoing) AUDIO_OUT else AUDIO_IN
            "file" -> if (outgoing) FILE_OUT else FILE_IN
            else -> if (outgoing) TEXT_OUT else TEXT_IN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TEXT_IN -> TextInVH(inflater.inflate(R.layout.item_msg_text_in, parent, false))
            TEXT_OUT -> TextOutVH(inflater.inflate(R.layout.item_msg_text_out, parent, false))
            IMG_IN -> ImageInVH(inflater.inflate(R.layout.item_msg_image_in, parent, false))
            IMG_OUT -> ImageOutVH(inflater.inflate(R.layout.item_msg_image_out, parent, false))
            AUDIO_IN -> AudioInVH(inflater.inflate(R.layout.item_msg_audio_in, parent, false))
            AUDIO_OUT -> AudioOutVH(inflater.inflate(R.layout.item_msg_audio_out, parent, false))
            FILE_IN -> FileInVH(inflater.inflate(R.layout.item_msg_file_in, parent, false))
            else -> FileOutVH(inflater.inflate(R.layout.item_msg_file_out, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = messages[position]
        val time = formatTime(m.createdAt)

        when (holder) {
            is TextInVH -> {
                bindAvatar(holder.ivAvatar, m.sender?.avatarUrl)
                holder.tvSender.visibility = if (isGroup) View.VISIBLE else View.GONE
                holder.tvSender.text = m.sender?.username ?: ""
                holder.tvText.text = m.text
                holder.tvTime.text = time
            }
            is TextOutVH -> {
                holder.tvText.text = m.text
                holder.tvTime.text = time
            }
            is ImageInVH -> {
                bindAvatar(holder.ivAvatar, m.sender?.avatarUrl)
                holder.tvSender.visibility = if (isGroup) View.VISIBLE else View.GONE
                holder.tvSender.text = m.sender?.username ?: ""

                val url = m.fileUrl
                if (!url.isNullOrBlank()) {
                    Glide.with(holder.ivImage.context).load(url).centerCrop().into(holder.ivImage)
                    holder.ivImage.setOnClickListener { onOpenUrl(url, m.mimeType ?: "image/*") }
                } else {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                if (m.text.isNotBlank()) {
                    holder.tvCaption.visibility = View.VISIBLE
                    holder.tvCaption.text = m.text
                } else {
                    holder.tvCaption.visibility = View.GONE
                }

                holder.tvTime.text = time
            }
            is ImageOutVH -> {
                val url = m.fileUrl
                if (!url.isNullOrBlank()) {
                    Glide.with(holder.ivImage.context).load(url).centerCrop().into(holder.ivImage)
                    holder.ivImage.setOnClickListener { onOpenUrl(url, m.mimeType ?: "image/*") }
                } else {
                    holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                if (m.text.isNotBlank()) {
                    holder.tvCaption.visibility = View.VISIBLE
                    holder.tvCaption.text = m.text
                } else {
                    holder.tvCaption.visibility = View.GONE
                }

                holder.tvTime.text = time
            }
            is AudioInVH -> {
                bindAvatar(holder.ivAvatar, m.sender?.avatarUrl)
                holder.tvSender.visibility = if (isGroup) View.VISIBLE else View.GONE
                holder.tvSender.text = m.sender?.username ?: ""

                val playing = VoiceNotePlayer.isPlaying(m.id)
                holder.btnPlay.setImageResource(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                holder.tvLabel.text = formatAudioLabel(m.durationMs)
                holder.tvTime.text = time

                val url = m.fileUrl
                holder.btnPlay.setOnClickListener {
                    if (!url.isNullOrBlank()) {
                        VoiceNotePlayer.toggle(m.id, url)
                        notifyDataSetChanged()
                    }
                }
                holder.itemView.setOnClickListener {
                    if (!url.isNullOrBlank()) {
                        VoiceNotePlayer.toggle(m.id, url)
                        notifyDataSetChanged()
                    }
                }
            }
            is AudioOutVH -> {
                val playing = VoiceNotePlayer.isPlaying(m.id)
                holder.btnPlay.setImageResource(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                holder.tvLabel.text = formatAudioLabel(m.durationMs)
                holder.tvTime.text = time

                val url = m.fileUrl
                holder.btnPlay.setOnClickListener {
                    if (!url.isNullOrBlank()) {
                        VoiceNotePlayer.toggle(m.id, url)
                        notifyDataSetChanged()
                    }
                }
                holder.itemView.setOnClickListener {
                    if (!url.isNullOrBlank()) {
                        VoiceNotePlayer.toggle(m.id, url)
                        notifyDataSetChanged()
                    }
                }
            }
            is FileInVH -> {
                bindAvatar(holder.ivAvatar, m.sender?.avatarUrl)
                holder.tvSender.visibility = if (isGroup) View.VISIBLE else View.GONE
                holder.tvSender.text = m.sender?.username ?: ""

                holder.tvFileName.text = m.fileName ?: "Файл"
                holder.tvTime.text = time
                val url = m.fileUrl
                holder.itemView.setOnClickListener {
                    if (!url.isNullOrBlank()) onOpenUrl(url, m.mimeType)
                }
            }
            is FileOutVH -> {
                holder.tvFileName.text = m.fileName ?: "Файл"
                holder.tvTime.text = time
                val url = m.fileUrl
                holder.itemView.setOnClickListener {
                    if (!url.isNullOrBlank()) onOpenUrl(url, m.mimeType)
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        ids.clear()
        messages.addAll(newMessages)
        newMessages.forEach { ids.add(it.id) }
        notifyDataSetChanged()
    }

    fun addMessage(m: Message) {
        if (ids.contains(m.id)) return
        ids.add(m.id)
        messages.add(m)
        notifyItemInserted(messages.lastIndex)
    }

    private fun bindAvatar(iv: CircleImageView, url: String?) {
        if (!url.isNullOrBlank()) {
            Glide.with(iv.context).load(url).circleCrop().into(iv)
        } else {
            iv.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun formatTime(ms: Long): String {
        return DateFormat.format("HH:mm", Date(ms)).toString()
    }

    private fun formatAudioLabel(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0) return "Аудио"
        val totalSec = (durationMs / 1000).toInt().coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }
}
