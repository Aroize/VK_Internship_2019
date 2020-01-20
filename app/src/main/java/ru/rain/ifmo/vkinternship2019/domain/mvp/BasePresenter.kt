package ru.rain.ifmo.vkinternship2019.domain.mvp

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
abstract class BasePresenter<T : MvpView> {

    protected var viewState: T? = null
    protected var showSpinner = false

    open fun attach(view: T) {
        viewState = view
        viewState?.recoverState(showSpinner)
    }

    open fun detach() {
        viewState = null
    }
}