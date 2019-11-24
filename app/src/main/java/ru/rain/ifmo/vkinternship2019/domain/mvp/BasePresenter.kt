package ru.rain.ifmo.vkinternship2019.domain.mvp

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
abstract class BasePresenter<T : MvpView> {

    protected var viewState: T? = null

    open fun attach(view: T) {
        viewState = view
        viewState?.recoverState(getState())
    }

    abstract fun getState(): MvpState

    open fun detach() {
        viewState = null
    }
}