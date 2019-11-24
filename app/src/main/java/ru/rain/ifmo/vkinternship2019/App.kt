package ru.rain.ifmo.vkinternship2019

import android.app.Application
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainPresenter

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
class App: Application() {
    companion object {
        val mainPresenter: MainPresenter = MainPresenter()
    }
}