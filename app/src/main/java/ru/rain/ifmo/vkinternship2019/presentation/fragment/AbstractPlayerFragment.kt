package ru.rain.ifmo.vkinternship2019.presentation.fragment

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.Log
import androidx.fragment.app.Fragment
import ru.rain.ifmo.vkinternship2019.domain.PlayerService

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
abstract class AbstractPlayerFragment: Fragment() {

    protected abstract var parentId: String

    protected lateinit var mediaBrowser: MediaBrowser

    private val mediaBrowserConnectionCallback = object : MediaBrowser.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.d("SESSION_CALLBACK", "MediaBrowser Connected")
            mediaBrowser.subscribe(parentId, mediaBrowserSubscriptionCallback)
            mediaController = MediaController(context as Context, mediaBrowser.sessionToken)
            mediaController.registerCallback(mediaControllerCallback)
            swapImage()
            mediaController.metadata?.let { updateInfo(it) }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d("SESSION_CALLBACK", "MediaBrowser connection failed")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Log.d("SESSION_CALLBACK", "Connection suspended")
        }
    }

    protected val mediaBrowserSubscriptionCallback = object : MediaBrowser.SubscriptionCallback() {}

    protected lateinit var mediaController: MediaController

    protected val mediaControllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            Log.d("SESSION_CALLBACK", "metadata changed")
            metadata ?: return
            updateInfo(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state ?: return
            swapImage()
        }
    }

    protected abstract fun updateInfo(mediaMetadata: MediaMetadata)

    protected abstract fun swapImage()

    override fun onStart() {
        super.onStart()
        mediaBrowser = MediaBrowser(context,
            ComponentName(context as Context, PlayerService::class.java),
            mediaBrowserConnectionCallback,
            null
        )
        mediaBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        mediaController.unregisterCallback(mediaControllerCallback)
        mediaBrowser.unsubscribe(parentId)
        mediaBrowser.disconnect()
    }
}