package com.soundwire

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket

object SocketManager {

    @Volatile private var socket: Socket? = null
    @Volatile private var lastUrl: String? = null
    @Volatile private var lastToken: String? = null

    fun get(context: Context): Socket {
        val appCtx = context.applicationContext
        val url = ServerConfig.getSocketUrl(appCtx)
        val token = SessionManager(appCtx).token()

        if (socket == null || lastUrl != url || lastToken != token) {
            synchronized(this) {
                if (socket == null || lastUrl != url || lastToken != token) {
                    try {
                        socket?.disconnect()
                        socket?.off()
                    } catch (_: Exception) {}

                    val opts = IO.Options().apply {
                        // Передаём токен как query: ?token=...
                        query = if (token.isNullOrBlank()) null else "token=$token"
                        reconnection = true
                        forceNew = true
                    }

                    socket = IO.socket(url, opts)
                    lastUrl = url
                    lastToken = token
                }
            }
        }

        if (socket?.connected() != true) {
            socket?.connect()
        }

        return socket!!
    }

    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
        } catch (_: Exception) {}
        socket = null
        lastUrl = null
        lastToken = null
    }
}
