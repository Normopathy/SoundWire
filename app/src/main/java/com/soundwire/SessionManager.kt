package com.soundwire

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("soundwire_prefs", Context.MODE_PRIVATE)

    fun saveSession(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_STATUS, user.status ?: "")
            .putString(KEY_AVATAR, user.avatarUrl ?: "")
            .putLong(KEY_LAST_SEEN, user.lastSeen ?: 0L)
            .apply()
    }

    fun updateCachedUser(user: User) {
        prefs.edit()
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_STATUS, user.status ?: "")
            .putString(KEY_AVATAR, user.avatarUrl ?: "")
            .putLong(KEY_LAST_SEEN, user.lastSeen ?: 0L)
            .apply()
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = token() != null

    fun userId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun cachedUser(): User? {
        val id = userId()
        if (id <= 0) return null
        val lastSeen = prefs.getLong(KEY_LAST_SEEN, 0L).let { if (it <= 0L) null else it }
        return User(
            id = id,
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            status = prefs.getString(KEY_STATUS, "")?.ifBlank { null },
            avatarUrl = prefs.getString(KEY_AVATAR, "")?.ifBlank { null },
            lastSeen = lastSeen
        )
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_USERNAME)
            .remove(KEY_STATUS)
            .remove(KEY_AVATAR)
            .remove(KEY_LAST_SEEN)
            .apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_USERNAME = "username"
        const val KEY_STATUS = "status"
        const val KEY_AVATAR = "avatar"
        const val KEY_LAST_SEEN = "last_seen"
    }
}
