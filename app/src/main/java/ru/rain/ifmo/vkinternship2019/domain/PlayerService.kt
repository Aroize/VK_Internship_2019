package ru.rain.ifmo.vkinternship2019.domain

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.media.MediaBrowserService
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.song.Song
import ru.rain.ifmo.vkinternship2019.data.song.SongSingleton

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 25.11.2019
 */
class PlayerService : MediaBrowserService() {

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return null
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {

    }

    companion object {
        const val SEEK_EXTRA = "seek.value"
        const val PLAYER_EVENT_EXTRA = "player.event.value"
        private const val NOTIFICATION_ID = 105
        private const val NOTIFICATION_CHANNEL_ID = "music.channel"
        private lateinit var exoPlayer: ExoPlayer

        @JvmStatic
        fun isPlaying(): Boolean {
            if (!this::exoPlayer.isInitialized)
                return false
            return exoPlayer.isPlaying
        }
    }

    private lateinit var mediaSession: MediaSession
    private var mediaCallback = object : MediaSession.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.d("MEDIA_SESSION", "onPlay override")
        }
    }

    private lateinit var intentArray: Array<Intent>
    private lateinit var pendingIntentArray: Array<PendingIntent>
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSourceFactory: ProgressiveMediaSource.Factory
    private val songSingleton = SongSingleton.instance
    private var newPlaylist = true

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        intentArray = Array(3) {
            when (it) {
                0 -> Intent(this, PlayerService::class.java).apply {
                    putExtra(PLAYER_EVENT_EXTRA, PlayerEvent.PLAY_PAUSE.toInt())
                }
                1 -> Intent(this, PlayerService::class.java).apply {
                    putExtra(PLAYER_EVENT_EXTRA, PlayerEvent.NEXT.toInt())
                }
                else -> Intent(this, PlayerService::class.java).apply {
                    putExtra(PLAYER_EVENT_EXTRA, PlayerEvent.PREV.toInt())
                }
            }
        }
        pendingIntentArray = Array(3) { PendingIntent.getService(this, it, intentArray[it], PendingIntent.FLAG_UPDATE_CURRENT)}
        initializePlayer()
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.getIntExtra(PLAYER_EVENT_EXTRA, -1)) {
                PlayerEvent.PREV.toInt() -> {
                    previous()
                }
                PlayerEvent.NEXT.toInt() -> {
                    next()
                }
                PlayerEvent.PLAY_PAUSE.toInt() -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.playWhenReady = false
                    } else {
                        play()
                    }
                }
                PlayerEvent.NEW_PLAYLIST.toInt() -> {
                    newPlaylist = true
                    exoPlayer.playWhenReady = false
                    notificationManager.notify(NOTIFICATION_ID, buildNotification())
                }
                PlayerEvent.SEEK.toInt() -> {
                    //TODO seek later
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun play() {
        if (exoPlayer.duration < 0 || newPlaylist) {
            //Media source is not loaded
            newPlaylist = false
            val mediaSource = mediaSourceFactory.createMediaSource(songSingleton.currentSong().uri)
            exoPlayer.prepare(mediaSource)
        }
        exoPlayer.playWhenReady = true
    }

    private fun next() {
        val mediaSource = mediaSourceFactory.createMediaSource(songSingleton.nextSong().uri)
        exoPlayer.prepare(mediaSource)
        notifyChangedResource()
    }

    private fun previous() {
        if (exoPlayer.currentPosition < 2000) {
            val mediaSource = mediaSourceFactory.createMediaSource(songSingleton.prevSong().uri)
            exoPlayer.prepare(mediaSource)
        } else {
            exoPlayer.seekTo(0)
        }
        notifyChangedResource()
    }

    private fun notifyChangedResource() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
        sendBroadcast(Intent(PlayerService::class.java.`package`.toString()))
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
        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        if (exoPlayer.isPlaying) {
            notificationLayout.setImageViewResource(R.id.notif_pause, R.drawable.ic_pause_28)
        } else {
            notificationLayout.setImageViewResource(R.id.notif_pause, R.drawable.ic_play_48)
        }
        val song = songSingleton.currentSong()
        notificationLayout.setTextViewText(R.id.notif_name, song.name)
        notificationLayout.setTextViewText(R.id.notif_artist, song.author)
        if (song.albumImage == null) {
            notificationLayout.setImageViewResource(R.id.notif_album, R.drawable.itunes_no_artwork)
        } else {
            notificationLayout.setImageViewBitmap(R.id.notif_album, song.albumImage)
        }
        notificationLayout.setOnClickPendingIntent(R.id.notif_pause, pendingIntentArray[0])
        notificationLayout.setOnClickPendingIntent(R.id.notif_next, pendingIntentArray[1])
        notificationLayout.setOnClickPendingIntent(R.id.notif_prev, pendingIntentArray[2])
        return builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomBigContentView(notificationLayout)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
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
        exoPlayer.addListener(object : Player.EventListener{
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> notifyChangedResource()
                    Player.STATE_ENDED -> next()
                }
            }
        })
    }
}