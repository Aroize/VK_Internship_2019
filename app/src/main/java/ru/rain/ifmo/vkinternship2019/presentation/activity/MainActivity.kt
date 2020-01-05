package ru.rain.ifmo.vkinternship2019.presentation.activity

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import ru.rain.ifmo.vkinternship2019.App
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder
import ru.rain.ifmo.vkinternship2019.data.song.Song
import ru.rain.ifmo.vkinternship2019.domain.PlayerEvent
import ru.rain.ifmo.vkinternship2019.domain.PlayerService
import ru.rain.ifmo.vkinternship2019.domain.mvp.MainState
import ru.rain.ifmo.vkinternship2019.domain.mvp.MvpState
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
    }

    private lateinit var md: MediaBrowser
    private lateinit var mc: MediaController

    private var spinnerDialog: SpinnerDialog =
        SpinnerDialog()

    private var loadSongs = false

    private val presenter: MainPresenter by lazy(LazyThreadSafetyMode.NONE) { App.mainPresenter }

    private lateinit var receiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        md = MediaBrowser(this,
            ComponentName(this, PlayerService::class.java),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    super.onConnected()
                    Log.d("MEDIA_SESSION", "Connected")
                    md.subscribe("ID",
                        object : MediaBrowser.SubscriptionCallback() {
                            override fun onChildrenLoaded(
                                parentId: String,
                                children: MutableList<MediaBrowser.MediaItem>
                            ) {
                                super.onChildrenLoaded(parentId, children)
                                Log.d("MEDIA_SESSION", "onChildrenLoaded")
                            }
                        }
                    )
                    mc = MediaController(this@MainActivity, md.sessionToken)
                    mc.registerCallback(object : MediaController.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackState?) {
                            super.onPlaybackStateChanged(state)
                            Log.d("MEDIA_SESSION", "State : $state")
                        }
                    })
                }
            },
            null
            )
        md.connect()

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
        receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                p1 ?: return
                if (p1.action == PlayerService::class.java.`package`.toString()) {
                    presenter.updateState()
                }
            }
        }
        registerReceiver(receiver, IntentFilter(PlayerService::class.java.`package`.toString()))
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
            presenter.loadSongs()
        }
    }

    override fun onPause() {
        super.onPause()
        if (spinnerDialog.isAdded)
            spinnerDialog.dismiss()
        presenter.detach()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun recoverState(state: MvpState) {
        if (state is MainState) {
            if (state.showSpinner) {
                showSpinner()
            } else {
                dismissSpinner()
            }
            state.song ?: return
            updateSongInfo(state.song, state.isPlaying)
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
        choose_folder_btn.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MainPlayerFragment())
            .commitNow()
    }

    override fun showMiniPlayer() {
        choose_folder_btn.visibility = View.VISIBLE
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        if (fragment == null) {
            val miniPlayer = MiniPlayerFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, miniPlayer)
                .commitNow()
            val yAnimator = ValueAnimator.ofFloat(resources.getDimension(R.dimen.mini_player_height), 0f)
            yAnimator.duration = 1_000
            yAnimator.addUpdateListener {
                miniPlayer.rootView.y = it.animatedValue as Float
                miniPlayer.rootView.requestLayout()
            }
            val set = AnimatorSet()
            set.interpolator = LinearInterpolator()
            set.play(yAnimator)
            set.start()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MiniPlayerFragment())
                .commitNow()
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

    override fun updateSongInfo(song: Song, isPlaying: Boolean) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? AbstractPlayerFragment
        fragment ?: return
        fragment.updateInfo(song, isPlaying)
    }

    fun onPlayerEvent(event: PlayerEvent, seekValue : Int = 0) {
        presenter.onPlayerEvent(event, seekValue)
    }

    fun swapPlayer() = presenter.swapPlayer()
}
