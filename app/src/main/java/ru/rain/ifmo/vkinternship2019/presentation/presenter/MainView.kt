package ru.rain.ifmo.vkinternship2019.presentation.presenter

import ru.rain.ifmo.vkinternship2019.data.MusicFolder
import ru.rain.ifmo.vkinternship2019.data.Song
import ru.rain.ifmo.vkinternship2019.domain.MvpView

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
interface MainView: MvpView {
    fun openExplorer(list: ArrayList<MusicFolder>)

    fun showSpinner()

    fun dismissSpinner()

    fun showMiniPlayer(song: Song)

    fun showMainPlayer(song: Song)

    fun hidePlayer()

    fun updateSongInfo(song: Song)
}