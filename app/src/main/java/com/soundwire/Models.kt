package com.soundwire

import com.google.firebase.Timestamp

// Модель пользователя
data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String? = null,
    val status: String = "В сети",
    val phoneNumber: String? = null,
    val contacts: List<String> = emptyList(),
    val chatIds: List<String> = emptyList(),
    val lastSeen: Timestamp = Timestamp.now(),
    val fcmToken: String? = null
)

// Модель чата
data class Chat(
    val chatId: String = "",
    val type: String = "private",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp = Timestamp.now(),
    val chatName: String? = null,
    val chatImage: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val unreadCount: Map<String, Int> = emptyMap()
)

// Модель сообщения для адаптера
data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Timestamp,
    val avatarUrl: String? = null
)

// Модель запроса на добавление в контакты
data class ContactRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending",
    val timestamp: Timestamp = Timestamp.now(),
    val message: String = ""
)