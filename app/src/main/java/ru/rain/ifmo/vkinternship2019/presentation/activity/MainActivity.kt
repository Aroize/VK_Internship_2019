package ru.rain.ifmo.vkinternship2019.presentation.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import ru.rain.ifmo.vkinternship2019.App
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.MusicFolder
import ru.rain.ifmo.vkinternship2019.data.Song
import ru.rain.ifmo.vkinternship2019.data.SpinnerDialog
import ru.rain.ifmo.vkinternship2019.domain.AbstractPlayerFragment
import ru.rain.ifmo.vkinternship2019.domain.MainState
import ru.rain.ifmo.vkinternship2019.domain.MvpState
import ru.rain.ifmo.vkinternship2019.presentation.fragment.MiniPlayerFragment
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainPresenter
import ru.rain.ifmo.vkinternship2019.presentation.presenter.MainView

class MainActivity : AppCompatActivity(), MainView {

    companion object {
        private const val PERMISSION_WRITE_EXTERNAL = 101
        private const val REQUEST_PICK_FOLDER = 102
        private const val SPINNER_TAG = "spinner.tag"
    }

    private var spinnerDialog: SpinnerDialog = SpinnerDialog()

    private var loadSongs = false

    private val presenter: MainPresenter by lazy(LazyThreadSafetyMode.NONE) { App.mainPresenter }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
            presenter.loadSongs(applicationContext)
        }
    }

    override fun onPause() {
        super.onPause()
        if (spinnerDialog.isAdded)
            spinnerDialog.dismiss()
        presenter.detach()
    }

    override fun recoverState(state: MvpState) {
        if (state is MainState) {
            if (state.showSpinner) {
                showSpinner()
            } else {
                dismissSpinner()
            }
            state.song ?: return
            updateSongInfo(state.song)
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

    override fun showMainPlayer(song: Song) {

    }

    override fun showMiniPlayer(song: Song) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, MiniPlayerFragment())
                .commitNow()
            updateSongInfo(song)
        }
    }

    override fun hidePlayer() {

    }

    override fun updateSongInfo(song: Song) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        fragment ?: return
        fragment.updateInfo(song)
    }
}
