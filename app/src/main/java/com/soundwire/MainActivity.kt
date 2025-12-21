package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.soundwire.databinding.ActivityMainBinding
import com.soundwire.ui.ChatsFragment
import com.soundwire.ui.ContactsFragment
import com.soundwire.ui.MusicFragment
import com.soundwire.ui.ProfileFragment

class MainActivity : AppCompatActivity() {

    enum class Tab { CHATS, CONTACTS, MUSIC, PROFILE }

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        if (!session.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Инициализация общих синглтонов
        PresenceRepository.init(this)
        PlayerManager.init(this)

        setupMiniPlayer()

        // По умолчанию открываем список чатов (как Telegram)
        if (savedInstanceState == null) {
            selectTab(Tab.CHATS)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    showFragment(ChatsFragment())
                    true
                }
                R.id.nav_contacts -> {
                    showFragment(ContactsFragment())
                    true
                }
                R.id.nav_music -> {
                    showFragment(MusicFragment())
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun selectTab(tab: Tab) {
        binding.bottomNav.selectedItemId = when (tab) {
            Tab.CHATS -> R.id.nav_chats
            Tab.CONTACTS -> R.id.nav_contacts
            Tab.MUSIC -> R.id.nav_music
            Tab.PROFILE -> R.id.nav_profile
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    private fun setupMiniPlayer() {
        // ВНИМАНИЕ:
        // view_mini_player.xml подключён через <include>.
        // ActivityMainBinding не всегда генерирует прямые поля для include-лейаута,
        // поэтому берём ссылки через findViewById.

        val miniPlayer = binding.root.findViewById<View>(R.id.miniPlayer)
        val btnPlayPause = binding.root.findViewById<ImageButton>(R.id.btnMiniPlayPause)
        val btnNext = binding.root.findViewById<ImageButton>(R.id.btnMiniNext)
        val btnPrev = binding.root.findViewById<ImageButton>(R.id.btnMiniPrev)
        val tvTitle = binding.root.findViewById<TextView>(R.id.tvMiniTitle)
        val tvSubtitle = binding.root.findViewById<TextView>(R.id.tvMiniSubtitle)
        val ivCover = binding.root.findViewById<ImageView>(R.id.ivMiniCover)

        // click => открыть полный плеер
        miniPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        btnPlayPause.setOnClickListener { PlayerManager.togglePlayPause() }
        btnNext.setOnClickListener { PlayerManager.next() }
        btnPrev.setOnClickListener { PlayerManager.prev() }

        PlayerManager.currentTrack.observe(this) { track ->
            if (track == null) {
                miniPlayer.visibility = View.GONE
            } else {
                miniPlayer.visibility = View.VISIBLE
                tvTitle.text = track.title
                tvSubtitle.text = track.artist ?: when (track.source) {
                    TrackSource.LOCAL -> "На телефоне"
                    TrackSource.SERVER -> "С компьютера"
                }

                val cover = track.coverUrl
                if (!cover.isNullOrBlank()) {
                    Glide.with(this).load(cover).centerCrop().into(ivCover)
                } else {
                    ivCover.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }

        PlayerManager.isPlaying.observe(this) { playing ->
            btnPlayPause.setImageResource(
                if (playing == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Если приложение закрывается полностью — отключаем сокет
        if (isFinishing) {
            SocketManager.disconnect()
            PlayerManager.release()
            VoiceNotePlayer.stop()
        }
    }
}
