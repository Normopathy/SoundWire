package com.soundwire

import android.content.Context

/**
 * Хранит адрес backend-сервера (Node.js) в SharedPreferences.
 *
 * По умолчанию стоит 10.0.2.2 для Android Emulator.
 * Для реального телефона в локальной сети укажи IP компьютера, на котором запущен backend,
 * например: http://192.168.0.10:3000/
 */
object ServerConfig {
    private const val PREFS = "soundwire_prefs"
    private const val KEY_BASE_URL = "base_url"

    private const val DEFAULT_BASE_URL = "http://192.168.1.213:3000/"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val fixed = if (url.endsWith("/")) url else "$url/"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, fixed)
            .apply()
    }

    /** Socket.IO ожидает base без завершающего слеша */
    fun getSocketUrl(context: Context): String = getBaseUrl(context).removeSuffix("/")
}
