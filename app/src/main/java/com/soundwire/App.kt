package com.soundwire

import android.app.Application
import com.soundwire.player.PlayerCache

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализируем кеш ExoPlayer заранее (не обязательно, но ускоряет первый старт плеера)
        PlayerCache.get(this)
    }
}
