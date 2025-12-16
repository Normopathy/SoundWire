package com.soundwire

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.soundwire.databinding.ActivityChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private lateinit var adapter: ChatMessageAdapter
    private var chatId: String? = null
    private var chatType: String = "private"
    private var messagesListener: ListenerRegistration? = null
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("chatId")
        chatType = intent.getStringExtra("chatType") ?: "private"

        adapter = ChatMessageAdapter(messages)
        binding.messagesContainerRecycler.layoutManager = LinearLayoutManager(this)
        binding.messagesContainerRecycler.adapter = adapter

        if (chatId != null) {
            loadMessages()
            loadChatInfo()
        } else {
            val contactId = intent.getStringExtra("contactId")
            if (contactId != null) {
                createNewChat(contactId)
            }
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttach.setOnClickListener {
            openImagePicker()
        }
    }

    private fun createNewChat(contactId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = "$currentUserId-$contactId"

        val chat = Chat(
            chatId = chatId,
            type = "private",
            participants = listOf(currentUserId, contactId),
            createdBy = currentUserId
        )

        db.collection("chats").document(chatId).set(chat)
            .addOnSuccessListener {
                this.chatId = chatId
                loadMessages()
            }
    }

    private fun loadMessages() {
        chatId ?: return

        messagesListener = db.collection("chats").document(chatId!!)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val data = change.document.data

                    val message = ChatMessage(
                        sender = data["senderId"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        avatarUrl = data["avatarUrl"] as? String
                    )

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            messages.add(message)
                            adapter.notifyItemInserted(messages.size - 1)
                            binding.messagesContainerRecycler.scrollToPosition(messages.size - 1)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val index = messages.indexOfFirst { it.sender == message.sender && it.text == message.text }
                            if (index != -1) {
                                messages[index] = message
                                adapter.notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = messages.indexOfFirst { it.sender == message.sender && it.text == message.text }
                            if (index != -1) {
                                messages.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }
            }
    }

    private fun loadChatInfo() {
        chatId ?: return

        db.collection("chats").document(chatId!!).get()
            .addOnSuccessListener { document ->
                val chat = document.toObject(Chat::class.java)
                if (chat?.type == "private") {
                    binding.tvChatTitle.text = "Приватный чат"
                } else {
                    binding.tvChatTitle.text = chat?.chatName ?: "Групповой чат"
                }
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                sendImageMessage(selectedImageUri)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty() || chatId == null) return

        val currentUser = auth.currentUser ?: return
        val messageId = UUID.randomUUID().toString()

        val message = hashMapOf(
            "messageId" to messageId,
            "chatId" to chatId!!,
            "senderId" to currentUser.uid,
            "senderName" to currentUser.displayName ?: "",
            "text" to text,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "type" to "text",
            "avatarUrl" to currentUser.photoUrl?.toString()
        )

        db.collection("chats").document(chatId!!).collection("messages")
            .document(messageId).set(message)
            .addOnSuccessListener {
                // Создаем Map явно
                val updates: Map<String, Any> = hashMapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to com.google.firebase.Timestamp.now()
                )
                db.collection("chats").document(chatId!!).update(updates)
                binding.etMessage.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendImageMessage(imageUri: Uri) {
        chatId ?: return
        val currentUser = auth.currentUser ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val storageRef = storage.reference
                val imageRef = storageRef.child("chat_images/${chatId}/${UUID.randomUUID()}.jpg")

                imageRef.putFile(imageUri).await()
                val downloadUrl = imageRef.downloadUrl.await()

                val messageId = UUID.randomUUID().toString()
                val message = hashMapOf(
                    "messageId" to messageId,
                    "chatId" to chatId!!,
                    "senderId" to currentUser.uid,
                    "senderName" to currentUser.displayName ?: "",
                    "text" to "Изображение",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "type" to "image",
                    "mediaUrl" to downloadUrl.toString(),
                    "avatarUrl" to currentUser.photoUrl?.toString()
                )

                db.collection("chats").document(chatId!!).collection("messages")
                    .document(messageId).set(message).await()

                // Создаем Map явно
                val updates: Map<String, Any> = hashMapOf(
                    "lastMessage" to "Изображение",
                    "lastMessageTime" to com.google.firebase.Timestamp.now()
                )
                db.collection("chats").document(chatId!!).update(updates)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "Ошибка отправки изображения", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }
}