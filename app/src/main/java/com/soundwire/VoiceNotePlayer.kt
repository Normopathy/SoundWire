package com.soundwire

import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Простейший проигрыватель для голосовых/аудио-сообщений.
 * Играет ОДНО сообщение одновременно.
 */
object VoiceNotePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var currentMessageId: Long? = null

    private var onStateChanged: ((Long?, Boolean) -> Unit)? = null

    fun setOnStateChangedListener(listener: ((Long?, Boolean) -> Unit)?) {
        onStateChanged = listener
    }

    fun isPlaying(messageId: Long): Boolean {
        return currentMessageId == messageId && mediaPlayer?.isPlaying == true
    }

    fun toggle(messageId: Long, url: String) {
        if (currentMessageId == messageId && mediaPlayer?.isPlaying == true) {
            stop()
            return
        }

        stop()
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                currentMessageId = messageId
                it.start()
                onStateChanged?.invoke(currentMessageId, true)
            }
            mp.setOnCompletionListener {
                stop()
            }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            mp.prepareAsync()
            mediaPlayer = mp
            // Состояние "готовимся" не отображаем
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        val old = currentMessageId
        currentMessageId = null
        if (old != null) {
            onStateChanged?.invoke(old, false)
        }
    }
}
