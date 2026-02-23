package com.example.voicetodo.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.voicetodo.MainActivity

class VoicePlaybackService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_PLAY -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val contentText = intent.getStringExtra(EXTRA_CONTENT_TEXT).orEmpty()
                val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH).orEmpty()
                if (reminderId <= 0L || audioPath.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(notificationId(reminderId), buildForegroundNotification(reminderId, contentText))
                startPlayback(audioPath)
            }

            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startPlayback(audioPath: String) {
        stopPlayback()
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(audioPath)
                setOnCompletionListener {
                    stopPlayback()
                    stopSelf()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    stopSelf()
                    true
                }
                prepare()
                start()
            }
        }.onSuccess { created ->
            player = created
        }.onFailure {
            stopSelf()
        }
    }

    private fun stopPlayback() {
        player?.runCatching {
            stop()
        }
        player?.release()
        player = null
    }

    private fun buildForegroundNotification(reminderId: Long, contentText: String): Notification {
        ensureChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            reminderId.toInt() + 41_000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, VoicePlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            reminderId.toInt() + 42_000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("正在播放语音")
            .setContentText(contentText.ifBlank { "语音待办" })
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止播放", stopPending)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "语音播放",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "通知动作触发语音播放时使用"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(reminderId: Long): Int {
        return 70_000 + (reminderId % 10_000).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "todo_voice_playback_channel"
        private const val ACTION_PLAY = "com.example.voicetodo.voice.ACTION_PLAY"
        private const val ACTION_STOP = "com.example.voicetodo.voice.ACTION_STOP"
        private const val EXTRA_REMINDER_ID = "extra_voice_play_reminder_id"
        private const val EXTRA_CONTENT_TEXT = "extra_voice_play_content"
        private const val EXTRA_AUDIO_PATH = "extra_voice_play_audio_path"

        fun startPlayback(context: Context, reminderId: Long, contentText: String, audioPath: String) {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_CONTENT_TEXT, contentText)
                putExtra(EXTRA_AUDIO_PATH, audioPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
