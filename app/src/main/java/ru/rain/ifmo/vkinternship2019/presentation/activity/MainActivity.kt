package ru.rain.ifmo.vkinternship2019.presentation.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import ru.rain.ifmo.vkinternship2019.App
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder
import ru.rain.ifmo.vkinternship2019.domain.PlayerService
import ru.rain.ifmo.vkinternship2019.presentation.fragment.AbstractPlayerFragment
import ru.rain.ifmo.vkinternship2019.presentation.fragment.MainPlayerFragment
import ru.rain.ifmo.vkinternship2019.presentation.fragment.MiniPlayerFragment
import ru.rain.ifmo.vkinternship2019.presentation.fragment.SpinnerDialog
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainPresenter
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainView


class MainActivity : AppCompatActivity(), MainView {

    companion object {

        private const val PERMISSION_WRITE_EXTERNAL = 101

        private const val REQUEST_PICK_FOLDER = 102

        private const val SPINNER_TAG = "spinner.tag"

        private const val MAIN_DURATION_FULL = 1_000L

        private const val MINI_DURATION_FULL = 500L
    }

    private var spinnerDialog: SpinnerDialog =
        SpinnerDialog()

    private var loadSongs = false

    private val presenter: MainPresenter by lazy(LazyThreadSafetyMode.NONE) { App.mainPresenter }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is MainPlayerFragment) {
            choose_folder_btn.alpha = 0f
            choose_folder_btn.visibility = View.GONE
        }
        supportActionBar?.hide()
        choose_folder_btn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_WRITE_EXTERNAL
                )
            } else {
                presenter.pickFolder(contentResolver)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_WRITE_EXTERNAL) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_DENIED) {
                presenter.pickFolder(contentResolver)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            loadSongs = true
            val manager = LocalBroadcastManager.getInstance(this)
            manager.sendBroadcast(Intent(PlayerService.ACTION_NEW_PLAYLIST))
        }
    }

    override fun onStart() {
        super.onStart()
        val fragment = supportFragmentManager.findFragmentByTag(SPINNER_TAG)
        if (fragment != null)
            spinnerDialog = fragment as SpinnerDialog
        presenter.attach(this)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (loadSongs) {
            loadSongs = false
            presenter.loadSongs()
        }
    }

    override fun onStop() {
        super.onStop()
        if (spinnerDialog.isAdded)
            spinnerDialog.dismiss()
        presenter.detach()
    }

    override fun recoverState(showSpinner: Boolean) {
        if (showSpinner) {
            showSpinner()
        } else {
            dismissSpinner()
        }
    }

    override fun openExplorer(list: ArrayList<MusicFolder>) {
        startActivityForResult(
            FolderListActivity.createIntent(this, list), REQUEST_PICK_FOLDER)
    }

    override fun dismissSpinner() {
        if (spinnerDialog.isAdded)
            spinnerDialog.dismiss()
    }

    override fun showSpinner() {
        if (!spinnerDialog.isAdded) {
            spinnerDialog.show(supportFragmentManager, SPINNER_TAG)
        }
    }

    override fun showMainPlayer() {
        val miniFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as MiniPlayerFragment
        val miniAnimator = ObjectAnimator.ofFloat(miniFragment.rootView, "y", 0f, resources.getDimension(R.dimen.mini_player_height))
        miniAnimator.duration = MINI_DURATION_FULL
        miniAnimator.doOnEnd {
            choose_folder_btn.visibility = View.GONE
            val fragment = MainPlayerFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitNow()
            val mainView = fragment.fragmentView
            val yAnimator = ObjectAnimator.ofFloat(mainView, "y", displayHeight().toFloat(), 0f)
            yAnimator.duration = MAIN_DURATION_FULL
            val set = AnimatorSet()
            set.interpolator = LinearInterpolator()
            set.play(yAnimator)
            set.start()
        }
        val alphaAnimator = ObjectAnimator.ofFloat(choose_folder_btn, "alpha", 0f)
        alphaAnimator.duration = MINI_DURATION_FULL
        val set = AnimatorSet()
        set.playTogether(miniAnimator, alphaAnimator)
        set.interpolator = LinearInterpolator()
        set.start()
    }

    override fun showMiniPlayer() {
        choose_folder_btn.visibility = View.VISIBLE
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        if (fragment == null) {
            val miniPlayer = MiniPlayerFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, miniPlayer)
                .commitNow()
            val set = AnimatorSet()
            set.interpolator = LinearInterpolator()
            set.play(ObjectAnimator.ofFloat(
                miniPlayer.rootView,
                "y",
                resources.getDimension(R.dimen.mini_player_height),
                0f).also { it.duration = MINI_DURATION_FULL })
            set.start()
        } else if (fragment is MainPlayerFragment) {
            choose_folder_btn.alpha = 0f
            val set = AnimatorSet()
            val miniFragment = MiniPlayerFragment()
            set.interpolator = LinearInterpolator()
            val mainAnimator = ObjectAnimator.ofFloat(fragment.fragmentView, "y", fragment.fragmentView.y, displayHeight().toFloat())
            mainAnimator.duration = MAIN_DURATION_FULL / 2
            mainAnimator.doOnEnd {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, miniFragment)
                    .commitNow()
                val miniAnimator = ObjectAnimator.ofFloat(miniFragment.rootView, "y", resources.getDimension(R.dimen.mini_player_height), 0f)
                miniAnimator.duration = MINI_DURATION_FULL
                AnimatorSet().apply {
                    val alphaAnimator = ObjectAnimator.ofFloat(choose_folder_btn, "alpha", 1f).apply { duration = MINI_DURATION_FULL }
                    playTogether(miniAnimator, alphaAnimator)
                    interpolator = LinearInterpolator()
                    start()
                }
            }
            set.play(mainAnimator)
            set.start()
        }
    }

    override fun hidePlayer() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        fragment ?: return
        choose_folder_btn.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .remove(fragment)
            .commitNow()
    }

    fun swapPlayer() = presenter.swapPlayer()

    fun displayHeight(): Int {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }
}
