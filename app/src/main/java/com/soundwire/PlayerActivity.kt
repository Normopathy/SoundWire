package com.soundwire

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.soundwire.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var player: SimpleExoPlayer? = null

    companion object {
        fun start(ctx: Context, url: String, title: String, artist: String, cover: String) {
            val i = Intent(ctx, PlayerActivity::class.java)
            i.putExtra("url", url)
            i.putExtra("title", title)
            i.putExtra("artist", artist)
            i.putExtra("cover", cover)
            ctx.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url = intent.getStringExtra("url") ?: ""
        binding.tvTitle.text = intent.getStringExtra("title")
        binding.tvArtist.text = intent.getStringExtra("artist")
        player = SimpleExoPlayer.Builder(this).build()
        val media = MediaItem.fromUri(Uri.parse(url))
        player?.setMediaItem(media)
        player?.prepare()
        player?.playWhenReady = true

        binding.btnPause.setOnClickListener {
            player?.pause()
        }
        binding.btnPlay.setOnClickListener {
            player?.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
