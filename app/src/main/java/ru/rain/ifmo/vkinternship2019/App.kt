package ru.rain.ifmo.vkinternship2019

import android.annotation.SuppressLint
import android.app.Application
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainPresenter

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
class App: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mainPresenter: MainPresenter
    }

    override fun onCreate() {
        super.onCreate()
        mainPresenter = MainPresenter(applicationContext)
    }
}