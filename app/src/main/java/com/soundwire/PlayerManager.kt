package com.soundwire

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.soundwire.player.PlayerCache

/**
 * Единый плеер для всего приложения (полный плеер + мини-плеер).
 * PlayerActivity больше НЕ создаёт свой ExoPlayer — он использует PlayerManager.
 */
object PlayerManager {

    private var appContext: Context? = null
    private var player: ExoPlayer? = null

    private var queue: List<Track> = emptyList()
    private var startIndex: Int = 0

    private val _currentTrack = MutableLiveData<Track?>(null)
    val currentTrack: LiveData<Track?> = _currentTrack

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    fun init(context: Context) {
        if (player != null) return
        appContext = context.applicationContext

        // CacheDataSource: кеширование HTTP треков
        val upstream = DefaultDataSource.Factory(appContext!!)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(PlayerCache.get(appContext!!))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheFactory)

        player = ExoPlayer.Builder(appContext!!)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.postValue(isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrack()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    updateCurrentTrack()
                }
            }
        })
    }

    fun getPlayer(): ExoPlayer? = player

    fun setQueue(tracks: List<Track>, index: Int, autoPlay: Boolean = true) {
        val p = player ?: return
        if (tracks.isEmpty()) return

        queue = tracks
        startIndex = index.coerceIn(0, tracks.lastIndex)

        val items = tracks.map { MediaItem.fromUri(Uri.parse(it.uri)) }
        p.setMediaItems(items, startIndex, 0L)
        p.prepare()
        p.playWhenReady = autoPlay

        updateCurrentTrack()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun prev() {
        player?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
    }

    fun durationMs(): Long = player?.duration ?: 0L
    fun positionMs(): Long = player?.currentPosition ?: 0L

    fun currentIndex(): Int = player?.currentMediaItemIndex ?: startIndex

    fun currentQueue(): List<Track> = queue

    private fun updateCurrentTrack() {
        val idx = currentIndex()
        val t = queue.getOrNull(idx)
        _currentTrack.postValue(t)
    }

    fun release() {
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _currentTrack.postValue(null)
        _isPlaying.postValue(false)

        // кеш можно оставить, но если хотите освободить ресурсы:
        // PlayerCache.release()
    }
}
