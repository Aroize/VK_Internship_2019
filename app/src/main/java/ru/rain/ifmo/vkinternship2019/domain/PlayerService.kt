package ru.rain.ifmo.vkinternship2019.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.song.Song
import ru.rain.ifmo.vkinternship2019.data.song.SongSingleton

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 25.11.2019
 */
class PlayerService : Service() {

    companion object {
        const val SEEK_EXTRA = "seek.value"
        const val PLAYER_EVENT_EXTRA = "player.event.value"
        private const val NOTIFICATION_ID = 105
        private const val NOTIFICATION_CHANNEL_ID = "music.channel"
        private lateinit var exoPlayer: ExoPlayer

        fun isPlaying(): Boolean {
            if (!this::exoPlayer.isInitialized)
                return false
            return exoPlayer.isPlaying
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSourceFactory: ProgressiveMediaSource.Factory

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        initializePlayer()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.getIntExtra(PLAYER_EVENT_EXTRA, -1)) {
                PlayerEvent.PREV.toInt() -> {
                    exoPlayer.previous()
                }
                PlayerEvent.NEXT.toInt() -> {
                    exoPlayer.next()
                }
                PlayerEvent.PLAY_PAUSE.toInt() -> {
                    exoPlayer.playWhenReady = !exoPlayer.isPlaying
                }
                PlayerEvent.SEEK.toInt() -> {
                    //TODO seek later
                }
                PlayerEvent.LOAD_PLAYLIST.toInt() -> {
                    loadPlaylist()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID)
        exoPlayer.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        return builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("World")
            .setContentTitle("Hello")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)


            .build()
    }

    private fun loadPlaylist() {
        val songs = SongSingleton.instance.playList
        val concatMediaSource = ConcatenatingMediaSource()
        songs.forEach {
            concatMediaSource.addMediaSource(mediaSourceFactory.createMediaSource(it.uri))
        }
        exoPlayer.prepare(concatMediaSource)
    }

    private fun initializePlayer() {
        val userAgent = Util.getUserAgent(this, getString(R.string.app_name))
        mediaSourceFactory = ProgressiveMediaSource
            .Factory(DefaultDataSourceFactory(this, userAgent))
        val trackSelector = DefaultTrackSelector()
        val loadControl = DefaultLoadControl()
        val rendersFactory = DefaultRenderersFactory(this)
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this,
            rendersFactory, trackSelector, loadControl)
        exoPlayer.setForegroundMode(true)
    }
}