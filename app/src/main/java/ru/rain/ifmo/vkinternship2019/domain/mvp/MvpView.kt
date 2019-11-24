package ru.rain.ifmo.vkinternship2019.domain.mvp

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
interface MvpView {
    fun recoverState(state: MvpState)
}