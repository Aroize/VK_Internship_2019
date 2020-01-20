package ru.rain.ifmo.vkinternship2019.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.exoplayer2.*
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

    companion object {

        const val ACTION_NEW_PLAYLIST = "new.playlist"

        const val ACTION_GET_POSITION = "get.position"

        private const val NOTIFICATION_ID = 105

        private const val NOTIFICATION_CHANNEL_ID = "music.channel"

        private const val TAG = "ru.rain.ifmo.vkinternship2019.domain.PlayerService"
    }

    private val playlistReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1 ?: return
            if (p1.action == ACTION_NEW_PLAYLIST) {
                newPlaylist = true
                mediaSessionCallback.onPause()
                if (SongSingleton.instance.playList.isNotEmpty()) {
                    updateMetadata(SongSingleton.instance.currentSong())
                }
            }
            if (p1.action == ACTION_GET_POSITION) {
                updateMetadata(SongSingleton.instance.currentSong())
            }
        }
    }

    private val broadcastManager = LocalBroadcastManager.getInstance(baseContext)

    private var newPlaylist = false

    private lateinit var mediaSession: MediaSession

    private lateinit var stateBuilder: PlaybackState.Builder

    private var speed = 1f

    private lateinit var buttonIntents: Array<PendingIntent>

    private val mediaSessionCallback = object : MediaSession.Callback(), Player.EventListener {
        override fun onPlay() {
            Log.d("SESSION_CALLBACK", "onPlay called")
            if (exoPlayer.duration < 0L || newPlaylist) {
                Log.d("SESSION_CALLBACK", "new track is added to the player")
                newPlaylist = false
                startService(Intent(this@PlayerService, PlayerService::class.java))
                //It is new track so we should load his data
                val song = SongSingleton.instance.currentSong()
                val mediaSource = mediaSourceFactory.createMediaSource(song.uri)
                exoPlayer.prepare(mediaSource)
                updateMetadata(song)
            }
            exoPlayer.playWhenReady = true
            startForeground(NOTIFICATION_ID, buildNotification(SongSingleton.instance.currentSong()))
            mediaSession.setPlaybackState(stateBuilder
                .setState(PlaybackState.STATE_PLAYING,
                    exoPlayer.currentPosition,
                    speed).build())
        }

        override fun onPause() {
            Log.d("SESSION_CALLBACK", "onPause called")
            exoPlayer.playWhenReady = false
            mediaSession.setPlaybackState(stateBuilder
                .setState(PlaybackState.STATE_PAUSED, exoPlayer.currentPosition, speed)
                .build())
            notificationManager.notify(NOTIFICATION_ID, buildNotification(SongSingleton.instance.currentSong()))
            stopForeground(false)
        }

        override fun onStop() {
            super.onStop()
            Log.d("SESSION_CALLBACK", "onStop called")
        }

        override fun onSeekTo(pos: Long) {
            Log.d("SESSION_CALLBACK", "onSeekTo called with pos=$pos")
            exoPlayer.seekTo(pos)
        }

        override fun onSkipToNext() {
            Log.d("SESSION_CALLBACK", "onSkipToNext called")
            val song = SongSingleton.instance.nextSong()
            val mediaSource = mediaSourceFactory.createMediaSource(song.uri)
            exoPlayer.prepare(mediaSource)
            updateMetadata(song)
            notificationManager.notify(NOTIFICATION_ID, buildNotification(song))
        }

        override fun onSkipToPrevious() {
            Log.d("SESSION_CALLBACK", "onSkipToPrevious called")
            if (exoPlayer.currentPosition in 0..3000) {
                val song = SongSingleton.instance.prevSong()
                val mediaSource = mediaSourceFactory.createMediaSource(song.uri)
                exoPlayer.prepare(mediaSource)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(song))
            } else {
                exoPlayer.seekTo(0L)
            }
            updateMetadata(SongSingleton.instance.currentSong())
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    Log.d("SESSION_CALLBACK", "Song is ended. Playing next")
                    onSkipToNext()
                }
            }
        }
    }

    private fun buildNotification(song: Song) =
        NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID).apply {
            val notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
                setOnClickPendingIntent(R.id.notif_prev, buttonIntents[0])
                setOnClickPendingIntent(R.id.notif_pause, buttonIntents[1])
                setOnClickPendingIntent(R.id.notif_next, buttonIntents[2])
            }
            notificationLayout.setTextViewText(R.id.notif_artist, song.author)
            notificationLayout.setTextViewText(R.id.notif_name, song.name)
            if (song.albumImage == null) {
                notificationLayout.setImageViewResource(R.id.notif_album, R.drawable.itunes_no_artwork)
            } else {
                notificationLayout.setImageViewBitmap(R.id.notif_album, song.albumImage)
            }
            if (exoPlayer.playWhenReady) {
                notificationLayout.setImageViewResource(R.id.notif_pause, R.drawable.ic_pause_28)
            } else {
                notificationLayout.setImageViewResource(R.id.notif_pause, R.drawable.ic_play_48)
            }
            setAutoCancel(false)
            setCustomContentView(notificationLayout)
            setCustomBigContentView(notificationLayout)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.mipmap.ic_launcher)
        }.build()

    private fun updateMetadata(song: Song) {
        mediaSession.setMetadata(MediaMetadata.Builder()
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, song.albumImage)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.author)
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.name)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, song.length)
            .putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER,
                if (this::exoPlayer.isInitialized) exoPlayer.currentPosition else 0L) //No custom keys is allowed
            .build())
        notificationManager.notify(NOTIFICATION_ID, buildNotification(song))
    }

    private lateinit var exoPlayer: ExoPlayer

    private lateinit var notificationManager: NotificationManager

    private lateinit var mediaSourceFactory: ProgressiveMediaSource.Factory

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(getString(R.string.app_name), null)
    }

    override fun onLoadChildren(p0: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SESSION_CALLBACK", "Service onCreate() called")
        initializePlayer()
        buttonIntents = Array(3) {
            PendingIntent.getService(this,
            it,
            Intent(baseContext, PlayerService::class.java).apply { putExtra(TAG, it) },
            PendingIntent.FLAG_UPDATE_CURRENT)
        }
        broadcastManager.registerReceiver(playlistReceiver, IntentFilter(ACTION_NEW_PLAYLIST))
        broadcastManager.registerReceiver(playlistReceiver, IntentFilter(ACTION_GET_POSITION))
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        initializeSession()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getIntExtra(TAG, -1)) {
            //Watch onCreate with initialization of Pending Intents for ids
            0 -> { mediaSessionCallback.onSkipToPrevious() }
            1 -> {
                if (exoPlayer.isPlaying) {
                    mediaSessionCallback.onPause()
                } else {
                    mediaSessionCallback.onPlay()
                }
            }
            2 -> { mediaSessionCallback.onSkipToNext() }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initializeSession() {
        stateBuilder = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY)
        mediaSession = MediaSession(baseContext, TAG)
        mediaSession.setPlaybackState(stateBuilder.build())
        mediaSession.setCallback(mediaSessionCallback)
        sessionToken = mediaSession.sessionToken
        updateMetadata(SongSingleton.instance.currentSong())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SESSION_CALLBACK", "onDestroy was called")
        broadcastManager.unregisterReceiver(playlistReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
        exoPlayer.playWhenReady = false
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
        exoPlayer.addListener(mediaSessionCallback)
        speed = exoPlayer.playbackParameters.speed
    }
}