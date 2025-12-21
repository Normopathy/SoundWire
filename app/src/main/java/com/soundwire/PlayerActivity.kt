package com.soundwire

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide
import com.soundwire.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    private val uiHandler = Handler(Looper.getMainLooper())
    private var userSeeking = false

    private var tracks: List<Track> = emptyList()
    private var startIndex: Int = 0

    private var playerListener: Player.Listener? = null

    companion object {
        private const val EXTRA_TRACKS = "tracks_json"
        private const val EXTRA_INDEX = "index"

        fun start(ctx: Context, tracks: List<Track>, index: Int) {
            // Чтобы мини-плеер показывался сразу (без ожидания открытия Activity)
            PlayerManager.init(ctx)
            PlayerManager.setQueue(tracks, index, autoPlay = true)

            val i = Intent(ctx, PlayerActivity::class.java)
            // extras оставляем на всякий случай (совместимость/отладка)
            i.putExtra(EXTRA_TRACKS, Gson().toJson(tracks))
            i.putExtra(EXTRA_INDEX, index)
            ctx.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PlayerManager.init(this)

        readExtrasOrUseCurrentQueue()
        bindControls()
        attachPlayerCallbacks()

        updateTrackUi()
        updateDurationUi()
        startProgressLoop()
    }

    private fun readExtrasOrUseCurrentQueue() {
        val json = intent.getStringExtra(EXTRA_TRACKS)
        startIndex = intent.getIntExtra(EXTRA_INDEX, 0)

        if (!json.isNullOrBlank()) {
            val type = object : TypeToken<List<Track>>() {}.type
            tracks = Gson().fromJson(json, type)
            if (tracks.isNotEmpty()) {
                PlayerManager.setQueue(tracks, startIndex, autoPlay = true)
                return
            }
        }

        // если extras нет — значит открыли из мини-плеера
        tracks = PlayerManager.currentQueue()
        startIndex = PlayerManager.currentIndex()
        if (tracks.isEmpty()) {
            finish()
        }
    }

    private fun attachPlayerCallbacks() {
        val p = PlayerManager.getPlayer() ?: return

        // убираем старый listener (на всякий случай)
        playerListener?.let { p.removeListener(it) }

        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateTrackUi()
                updateDurationUi()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    updateTrackUi()
                    updateDurationUi()
                }
            }
        }

        playerListener = l
        p.addListener(l)

        updatePlayPauseIcon(p.isPlaying)
    }

    private fun bindControls() {
        binding.btnPlayPause.setOnClickListener { PlayerManager.togglePlayPause() }
        binding.btnNext.setOnClickListener { PlayerManager.next() }
        binding.btnPrev.setOnClickListener { PlayerManager.prev() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrent.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val pos = seekBar?.progress?.toLong() ?: 0L
                PlayerManager.seekTo(pos)
                userSeeking = false
            }
        })
    }

    private fun updateTrackUi() {
        val idx = PlayerManager.currentIndex()
        val t = PlayerManager.currentQueue().getOrNull(idx) ?: tracks.getOrNull(idx)
        if (t != null) {
            binding.tvTitle.text = t.title
            val cover = t.coverUrl
            if (!cover.isNullOrBlank()) {
                Glide.with(this).load(cover).centerCrop().into(binding.ivCover)
            } else {
                binding.ivCover.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            binding.tvArtist.text = t.artist ?: ""
        }
    }

    private fun updateDurationUi() {
        val dur = PlayerManager.durationMs()
        if (dur > 0) {
            binding.seekBar.max = dur.toInt()
            binding.tvDuration.text = formatTime(dur)
        } else {
            binding.seekBar.max = 0
            binding.tvDuration.text = "0:00"
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun startProgressLoop() {
        uiHandler.post(object : Runnable {
            override fun run() {
                val pos = PlayerManager.positionMs()
                if (!userSeeking) {
                    binding.seekBar.progress = pos.toInt()
                    binding.tvCurrent.text = formatTime(pos)
                }

                // duration может обновиться позже
                if (binding.seekBar.max == 0) {
                    val d = PlayerManager.durationMs()
                    if (d > 0) updateDurationUi()
                }

                uiHandler.postDelayed(this, 400)
            }
        })
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)

        val p = PlayerManager.getPlayer()
        playerListener?.let { l ->
            try {
                p?.removeListener(l)
            } catch (_: Exception) {
            }
        }
        playerListener = null

        // НЕ release() — плеер живёт в PlayerManager
    }
}
