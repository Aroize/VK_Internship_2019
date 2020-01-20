package ru.rain.ifmo.vkinternship2019.presentation.fragment

import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_mini_player.*
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.presentation.activity.MainActivity
import ru.rain.ifmo.vkinternship2019.toPlayerDuration

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 23.11.2019
 */
class MiniPlayerFragment : AbstractPlayerFragment() {

    override var parentId = "parent.id"

    lateinit var rootView: View

    private lateinit var albumImageView: ImageView

    private lateinit var title: TextView

    private lateinit var length: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false)
        rootView.setOnClickListener {
            (activity as MainActivity).swapPlayer()
        }
        albumImageView = rootView.findViewById(R.id.mini_album_image)
        albumImageView.clipToOutline = true
        title = rootView.findViewById(R.id.song_title)
        title.isSelected = true
        length = rootView.findViewById(R.id.song_length)
        rootView.findViewById<View>(R.id.mini_next_btn).setOnClickListener {
            Log.d("SESSION_CALLBACK", "next_btn: ${mediaController.playbackState?.state}")
            mediaController.transportControls.skipToNext()
        }
        rootView.findViewById<View>(R.id.pause_play_btn).setOnClickListener {
            Log.d("SESSION_CALLBACK", "play_btn: ${mediaController.playbackState?.state}")
            if (mediaController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                mediaController.transportControls.pause()
            } else {
                mediaController.transportControls.play()
            }
            swapImage()
        }
        return rootView
    }

    override fun updateInfo(mediaMetadata: MediaMetadata) {
        val bitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (bitmap == null) {
            albumImageView.setImageResource(R.drawable.itunes_no_artwork)
        } else {
            albumImageView.setImageBitmap(bitmap)
        }
        title.text = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        length.text = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toPlayerDuration()
    }

    override fun swapImage() {
        mediaController.playbackState?.let {
            if (it.state == PlaybackState.STATE_PLAYING) {
                pause_play_btn.setImageResource(R.drawable.ic_pause_28)
            } else {
                pause_play_btn.setImageResource(R.drawable.ic_play_48)
            }
        }
    }
}