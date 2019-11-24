package ru.rain.ifmo.vkinternship2019.domain

import ru.rain.ifmo.vkinternship2019.data.Song
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainPresenter

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
sealed class MvpState

data class MainState(val showSpinner: Boolean,
                     val player: MainPresenter.Player,
                     val song: Song? = null): MvpState()