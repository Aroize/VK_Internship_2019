package ru.rain.ifmo.vkinternship2019.presentation.fragment

import androidx.fragment.app.Fragment
import ru.rain.ifmo.vkinternship2019.data.song.Song

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
abstract class AbstractPlayerFragment: Fragment() {
    abstract fun updateInfo(song: Song, isPlaying: Boolean)
}