package com.soundwire

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaService : Service() {

    private var player: ExoPlayer? = null
    private val CHANNEL_ID = "soundwire_media_channel"
    private val NOTIFICATION_ID = 1
    private val db = Firebase.firestore

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        createNotificationChannel()

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                updateNotification()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                player?.play()
                updateNotification()
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotification()
            }
            ACTION_STOP -> {
                player?.stop()
                stopForeground(true)
                stopSelf()
            }
            else -> {
                // Воспроизведение нового трека
                val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
                val title = intent.getStringExtra("title") ?: "Track"
                val artist = intent.getStringExtra("artist") ?: "Artist"

                val media = MediaItem.fromUri(url)
                player?.setMediaItem(media)
                player?.prepare()
                player?.play()

                // Сохраняем в историю прослушиваний
                saveToRecentPlays(title, artist, url)

                val notification = buildNotification(title, artist)
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        return START_STICKY
    }

    private fun saveToRecentPlays(title: String, artist: String, url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map = hashMapOf(
                    "title" to title,
                    "artist" to artist,
                    "url" to url,
                    "playedAt" to Timestamp.now()
                )
                db.collection("recent_plays").add(map)
            } catch (_: Exception) {
                // Игнорируем исключение
            }
        }
    }

    private fun buildNotification(title: String, artist: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Создаем Intent для управления медиа
        val playIntent = Intent(this, MediaService::class.java).apply {
            action = ACTION_PLAY
        }
        val pauseIntent = Intent(this, MediaService::class.java).apply {
            action = ACTION_PAUSE
        }
        val stopIntent = Intent(this, MediaService::class.java).apply {
            action = ACTION_STOP
        }

        val pendingPlay = PendingIntent.getService(
            this, 1, playIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pendingPause = PendingIntent.getService(
            this, 2, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pendingStop = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // В зависимости от состояния воспроизведения показываем разные иконки
        val isPlaying = player?.isPlaying == true
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) "Пауза" else "Играть"
        val playPauseAction = if (isPlaying) pendingPause else pendingPlay

        // Используем доступные иконки Android
        // Вместо ic_media_stop используем ic_media_pause для кнопки остановки
        // или другую доступную иконку
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play) // Используем любую доступную иконку
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_previous, "Назад", null)
            .addAction(playPauseIcon, playPauseText, playPauseAction)
            .addAction(android.R.drawable.ic_delete, "Стоп", pendingStop) // Заменяем на ic_delete
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val title = player?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Трек"
        val artist = player?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Исполнитель"
        val notification = buildNotification(title, artist)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SoundWire Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            serviceChannel.description = "Канал для уведомлений о воспроизведении медиа"
            serviceChannel.setSound(null, null) // Убираем звук уведомления
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY = "com.soundwire.action.PLAY"
        const val ACTION_PAUSE = "com.soundwire.action.PAUSE"
        const val ACTION_STOP = "com.soundwire.action.STOP"
    }
}