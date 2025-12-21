package com.soundwire

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Простое хранилище online/offline.
 * Backend шлёт:
 * - presence_snapshot { onlineUserIds: [...] }
 * - presence_update { userId: 1, online: true/false, lastSeen?: ... }
 */
object PresenceRepository {

    private val _onlineIds = MutableLiveData<Set<Int>>(emptySet())
    val onlineIds: LiveData<Set<Int>> = _onlineIds

    @Volatile private var initialized = false

    private val onSnapshot = Emitter.Listener { args ->
        if (args.isEmpty()) return@Listener
        val obj = args[0] as? JSONObject ?: return@Listener
        val arr = obj.optJSONArray("onlineUserIds") ?: JSONArray()
        val set = mutableSetOf<Int>()
        for (i in 0 until arr.length()) {
            set.add(arr.optInt(i))
        }
        _onlineIds.postValue(set)
    }

    private val onUpdate = Emitter.Listener { args ->
        if (args.isEmpty()) return@Listener
        val obj = args[0] as? JSONObject ?: return@Listener
        val userId = obj.optInt("userId", -1)
        val online = obj.optBoolean("online", false)
        if (userId <= 0) return@Listener

        val current = _onlineIds.value ?: emptySet()
        val next = current.toMutableSet()
        if (online) next.add(userId) else next.remove(userId)
        _onlineIds.postValue(next)
    }

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val socket = SocketManager.get(context.applicationContext)
            socket.on("presence_snapshot", onSnapshot)
            socket.on("presence_update", onUpdate)

            // если уже подключены, запросим снимок
            if (socket.connected()) {
                socket.emit("presence_get")
            }
            initialized = true
        }
    }

    fun isOnline(userId: Int): Boolean {
        return _onlineIds.value?.contains(userId) == true
    }
}
