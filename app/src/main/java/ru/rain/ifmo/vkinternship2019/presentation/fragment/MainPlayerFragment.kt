package ru.rain.ifmo.vkinternship2019.presentation.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.song.Song

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 16.12.2019
 */
class MainPlayerFragment : AbstractPlayerFragment() {

    private lateinit var fragmentView: View

    private lateinit var authorName: TextView

    private val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val icon = fragmentView.findViewById<ImageView>(R.id.explicit_icon)
        authorName.maxWidth = fragmentView.findViewById<LinearLayout>(R.id.track_container).width - icon.width
        removeGlobalListener()
    }

    override fun updateInfo(song: Song, isPlaying: Boolean) {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_main_player, container, false)
        authorName = fragmentView.findViewById(R.id.author_name)
        fragmentView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return fragmentView
    }

    private fun removeGlobalListener() {
        fragmentView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }
}