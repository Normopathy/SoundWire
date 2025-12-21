package com.soundwire.player

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

/**
 * Простое кеширование аудио для ExoPlayer.
 *
 * Если трек проигрывается по HTTP с backend-сервера, ExoPlayer будет сохранять его куски в кеш.
 */
object PlayerCache {

    // 200 МБ кеша, дальше будет вытесняться LRU
    private const val MAX_CACHE_BYTES: Long = 200L * 1024L * 1024L

    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        val appCtx = context.applicationContext
        if (cache == null) {
            synchronized(this) {
                if (cache == null) {
                    val cacheDir = File(appCtx.cacheDir, "exo_audio_cache")
                    val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                    val dbProvider = ExoDatabaseProvider(appCtx)
                    cache = SimpleCache(cacheDir, evictor, dbProvider)
                }
            }
        }
        return cache!!
    }

    fun release() {
        try {
            cache?.release()
        } catch (_: Exception) {
        }
        cache = null
    }
}
