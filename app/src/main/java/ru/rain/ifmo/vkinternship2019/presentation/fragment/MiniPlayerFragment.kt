package ru.rain.ifmo.vkinternship2019.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.Song
import ru.rain.ifmo.vkinternship2019.domain.AbstractPlayerFragment
import ru.rain.ifmo.vkinternship2019.toPlayerDuration

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 23.11.2019
 */
class MiniPlayerFragment : AbstractPlayerFragment() {

    private lateinit var rootView: View
    private lateinit var albumImageView: ImageView
    private lateinit var title: TextView
    private lateinit var length: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false)
        albumImageView = rootView.findViewById(R.id.mini_album_image)
        albumImageView.clipToOutline = true
        title = rootView.findViewById(R.id.song_title)
        title.isSelected = true
        length = rootView.findViewById(R.id.song_length)
        return rootView
    }

    override fun updateInfo(song: Song) {
        if (song.albumImage == null) {
            albumImageView.setImageResource(R.drawable.itunes_no_artwork)
        } else {
            albumImageView.setImageBitmap(song.albumImage)
        }
        title.text = song.author
        length.text = song.length.toPlayerDuration()
    }
}