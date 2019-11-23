package ru.rain.ifmo.vkinternship2019.screen.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import ru.rain.ifmo.vkinternship2019.R

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 23.11.2019
 */
class MiniPlayerFragment : Fragment() {

    private lateinit var rootView: View
    private lateinit var albumImage: ImageView
    private lateinit var title: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false)
        albumImage = rootView.findViewById(R.id.mini_album_image)
        albumImage.clipToOutline = true
        title = rootView.findViewById(R.id.song_title)
        title.isSelected = true
        return rootView
    }
}