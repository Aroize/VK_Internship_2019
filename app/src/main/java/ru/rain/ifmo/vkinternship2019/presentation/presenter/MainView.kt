package ru.rain.ifmo.vkinternship2019.presentation.presenter

import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder
import ru.rain.ifmo.vkinternship2019.data.song.Song
import ru.rain.ifmo.vkinternship2019.domain.mvp.MvpView

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
interface MainView: MvpView {
    fun openExplorer(list: ArrayList<MusicFolder>)

    fun showSpinner()

    fun dismissSpinner()

    fun showMiniPlayer()

    fun showMainPlayer()

    fun hidePlayer()

    fun updateSongInfo(song: Song, isPlaying: Boolean)
}