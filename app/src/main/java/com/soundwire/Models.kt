package com.soundwire

/**
 * Модели для работы с локальным backend (Node.js + MySQL).
 * Firebase полностью убран.
 */

data class User(
    val id: Int,
    val email: String,
    val username: String,
    val status: String? = null,
    val avatarUrl: String? = null,
    val lastSeen: Long? = null
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class ApiOkResponse(
    val ok: Boolean,
    val already: Boolean? = null
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdateProfileRequest(
    val username: String,
    val status: String
)

data class UploadAvatarResponse(
    val avatarUrl: String
)

// ----------- CONTACTS -----------

data class SendContactRequest(
    val toUserId: Int
)

data class SendContactRequestResponse(
    val ok: Boolean,
    val requestId: Int? = null,
    val already: Boolean? = null,
    val accepted: Boolean? = null
)

data class ContactRequest(
    val id: Int,
    val status: String,
    val createdAt: Long? = null,
    val fromUser: User? = null,
    val toUser: User? = null
)

// ----------- CHATS -----------

data class CreateChatRequest(
    val otherUserId: Int
)

data class CreateGroupChatRequest(
    val title: String,
    val participantIds: List<Int>
)

data class AddParticipantsRequest(
    val userIds: List<Int>
)

/**
 * Чат в списке.
 * - private: otherUser != null
 * - group: title != null, membersCount != null
 */
data class ChatSummary(
    val id: Int,
    val type: String,
    val title: String? = null,
    val avatarUrl: String? = null,
    val membersCount: Int? = null,
    val otherUser: User? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val createdAt: Long? = null
)

/**
 * Сообщение.
 * type: text | image | audio | file
 */
data class Message(
    val id: Long,
    val chatId: Int,
    val senderId: Int,
    val type: String = "text",
    val text: String = "",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val createdAt: Long,
    val sender: User? = null
)

// Участник группы

data class ChatParticipant(
    val user: User,
    val role: String
)

data class UploadChatAvatarResponse(
    val avatarUrl: String
)

// ----------- MUSIC -----------

enum class TrackSource { LOCAL, SERVER }

data class Track(
    val id: String,
    val title: String,
    val artist: String? = null,
    val uri: String,
    val source: TrackSource,
    // Пока обложка опциональная (можно будет добавить позже через metadata)
    val coverUrl: String? = null
)
