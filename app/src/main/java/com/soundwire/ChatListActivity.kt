package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.soundwire.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatListBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var adapter: ChatListAdapter
    private val chatList = mutableListOf<Chat>()
    private var chatsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerChats.layoutManager = LinearLayoutManager(this)
        adapter = ChatListAdapter(chatList) { chat ->
            openChat(chat)
        }
        binding.recyclerChats.adapter = adapter

        binding.btnNewChat.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        loadChats()
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        chatsListener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                chatList.clear()
                snapshots?.documents?.forEach { document ->
                    document.toObject(Chat::class.java)?.let { chat ->
                        chatList.add(chat)
                    }
                }

                // Сортируем по времени последнего сообщения
                chatList.sortByDescending { it.lastMessageTime }
                adapter.notifyDataSetChanged()
            }
    }

    private fun openChat(chat: Chat) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chat.chatId)
        intent.putExtra("chatType", chat.type)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        chatsListener?.remove()
    }
}