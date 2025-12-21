package com.soundwire

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.databinding.ActivityChatBinding
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var session: SessionManager

    private var chatId: Int = -1
    private var chatTitle: String = ""
    private var chatType: String = "private"

    private var otherUserId: Int? = null

    private lateinit var socket: Socket

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatMessageAdapter

    // ---- Recording ----
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording: Boolean = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) uploadAttachment(uri, type = "image")
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) uploadAttachment(uri, type = "file")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        chatId = intent.getIntExtra("chatId", -1)
        chatTitle = intent.getStringExtra("title") ?: "Чат"
        chatType = intent.getStringExtra("chatType") ?: "private"

        if (chatId <= 0) {
            Toast.makeText(this, "chatId не задан", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        if (chatType == "group") {
            binding.tvTitle.setOnClickListener {
                val i = Intent(this, ParticipantsActivity::class.java)
                i.putExtra("chatId", chatId)
                i.putExtra("title", chatTitle)
                startActivity(i)
            }
        }

        binding.tvTitle.text = chatTitle

        // Presence
        PresenceRepository.init(this)
        PresenceRepository.onlineIds.observe(this) { updateSubtitle() }

        adapter = ChatMessageAdapter(
            messages = messages,
            currentUserId = session.userId(),
            isGroup = chatType == "group",
            onOpenUrl = { url, mime -> openUrl(url, mime) }
        )

        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter

        binding.btnSend.setOnClickListener { sendText() }
        binding.btnAttach.setOnClickListener { showAttachMenu() }
        binding.btnMic.setOnClickListener { toggleRecording() }

        // Socket
        socket = SocketManager.get(this)
        attachSocketListeners()

        if (!socket.connected()) {
            socket.connect()
        } else {
            joinRoom()
        }

        // данные
        loadParticipants()
        loadHistory()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket.emit("leave_chat", JSONObject().put("chatId", chatId))
        } catch (_: Exception) {}
        detachSocketListeners()
        VoiceNotePlayer.setOnStateChangedListener(null)
        VoiceNotePlayer.stop()
        stopRecordingIfNeeded(force = true)
    }

    private fun attachSocketListeners() {
        socket.on(Socket.EVENT_CONNECT, onConnect)
        socket.on("new_message", onNewMessage)
    }

    private fun detachSocketListeners() {
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off("new_message", onNewMessage)
    }

    private val onConnect = Emitter.Listener {
        joinRoom()
    }

    private fun joinRoom() {
        try {
            socket.emit("join_chat", JSONObject().put("chatId", chatId))
        } catch (_: Exception) {}
    }

    private val onNewMessage = Emitter.Listener { args ->
        if (args.isEmpty()) return@Listener
        val obj = args[0] as? JSONObject ?: return@Listener
        val msg = parseMessage(obj) ?: return@Listener
        if (msg.chatId != chatId) return@Listener

        runOnUiThread {
            adapter.addMessage(msg)
            binding.recyclerMessages.scrollToPosition(messages.lastIndex)
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            try {
                val history = ApiProvider.api(this@ChatActivity).getMessages(chatId, limit = 200)
                adapter.setMessages(history)
                if (history.isNotEmpty()) {
                    binding.recyclerMessages.scrollToPosition(history.lastIndex)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Не удалось загрузить историю: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadParticipants() {
        lifecycleScope.launch {
            try {
                val participants = ApiProvider.api(this@ChatActivity).getParticipants(chatId)
                if (chatType == "private") {
                    val me = session.userId()
                    val other = participants.firstOrNull { it.user.id != me }?.user
                    otherUserId = other?.id
                    binding.tvTitle.text = other?.username ?: chatTitle
                } else {
                    binding.tvSubtitle.text = "${participants.size} участников"
                }
                updateSubtitle()
            } catch (_: Exception) {
                // не критично
            }
        }
    }

    private fun updateSubtitle() {
        if (chatType == "group") {
            // в loadParticipants уже выставили базовый текст
            return
        }
        val otherId = otherUserId
        if (otherId == null) {
            binding.tvSubtitle.text = ""
            return
        }
        val online = PresenceRepository.isOnline(otherId)
        binding.tvSubtitle.text = if (online) "online" else "offline"
    }

    private fun sendText() {
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return

        try {
            val payload = JSONObject()
                .put("chatId", chatId)
                .put("text", text)
            socket.emit("send_message", payload)
            binding.etMessage.setText("")
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось отправить: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAttachMenu() {
        val items = arrayOf("Фото", "Файл")
        AlertDialog.Builder(this)
            .setTitle("Вложение")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> pickFile.launch(arrayOf("*/*"))
                }
            }
            .show()
    }

    private fun uploadAttachment(uri: Uri, type: String) {
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    UriUtils.copyToCacheFile(this@ChatActivity, uri)
                }

                val mime = contentResolver.getType(uri)

                val part = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody((mime ?: "application/octet-stream").toMediaTypeOrNull())
                )

                val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
                val textBody = "".toRequestBody("text/plain".toMediaTypeOrNull())

                // durationMs только для audio
                val durationBody = if (type == "audio") {
                    val duration = readDurationMs(file)
                    duration.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                } else {
                    null
                }

                ApiProvider.api(this@ChatActivity).sendMessageWithFile(
                    chatId = chatId,
                    type = typeBody,
                    text = textBody,
                    durationMs = durationBody,
                    file = part
                )
                // Сообщение придёт через socket (new_message) — дублей не будет, т.к. adapter дедупит по id
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Не удалось отправить файл: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openUrl(url: String, mime: String?) {
        try {
            val uri = Uri.parse(url)
            val i = Intent(Intent.ACTION_VIEW).apply {
                if (!mime.isNullOrBlank()) {
                    setDataAndType(uri, mime)
                } else {
                    data = uri
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(i)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // ---------------- Voice recording ----------------

    private fun toggleRecording() {
        if (isRecording) {
            stopRecordingIfNeeded(force = false)
            return
        }

        // Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO)
            return
        }

        startRecording()
    }

    private fun startRecording() {
        try {
            val outFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")

            val r = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }

            recorder = r
            recordingFile = outFile
            isRecording = true

            binding.btnMic.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Запись... Нажмите ещё раз чтобы остановить", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
        VoiceNotePlayer.setOnStateChangedListener(null)
        VoiceNotePlayer.stop()
            stopRecordingIfNeeded(force = true)
            Toast.makeText(this, "Не удалось начать запись: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecordingIfNeeded(force: Boolean) {
        if (!isRecording && !force) return

        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
        }

        val file = recordingFile
        recordingFile = null

        val shouldUpload = isRecording && file != null && file.exists() && file.length() > 0
        isRecording = false
        binding.btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)

        if (shouldUpload && file != null) {
            uploadRecordedVoice(file)
        }
    }

    private fun uploadRecordedVoice(file: File) {
        lifecycleScope.launch {
            try {
                val part = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                )
                val typeBody = "audio".toRequestBody("text/plain".toMediaTypeOrNull())
                val textBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                val duration = readDurationMs(file)
                val durationBody = duration.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                ApiProvider.api(this@ChatActivity).sendMessageWithFile(
                    chatId = chatId,
                    type = typeBody,
                    text = textBody,
                    durationMs = durationBody,
                    file = part
                )
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Не удалось отправить аудио: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readDurationMs(file: File): Long {
        return try {
            val r = MediaMetadataRetriever()
            r.setDataSource(file.absolutePath)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            r.release()
            dur
        } catch (_: Exception) {
            0L
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO) {
            val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (ok) startRecording() else Toast.makeText(this, "Нужно разрешение на микрофон", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseMessage(obj: JSONObject): Message? {
        return try {
            val senderObj = obj.optJSONObject("sender")
            val sender = if (senderObj != null) {
                User(
                    id = senderObj.optInt("id"),
                    email = senderObj.optString("email"),
                    username = senderObj.optString("username"),
                    status = senderObj.optString("status").ifBlank { null },
                    avatarUrl = senderObj.optString("avatarUrl").ifBlank { null },
                    lastSeen = senderObj.optLong("lastSeen").let { if (it <= 0L) null else it }
                )
            } else null

            Message(
                id = obj.optLong("id"),
                chatId = obj.optInt("chatId"),
                senderId = obj.optInt("senderId"),
                type = obj.optString("type", "text"),
                text = obj.optString("text", ""),
                fileUrl = obj.optString("fileUrl").ifBlank { null },
                fileName = obj.optString("fileName").ifBlank { null },
                mimeType = obj.optString("mimeType").ifBlank { null },
                durationMs = obj.optLong("durationMs").let { if (it <= 0L) null else it },
                createdAt = obj.optLong("createdAt"),
                sender = sender
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val REQ_RECORD_AUDIO = 910
    }
}
