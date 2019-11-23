package ru.rain.ifmo.vkinternship2019.screen.activity

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.SongSingleton
import ru.rain.ifmo.vkinternship2019.data.SpinnerDialog
import ru.rain.ifmo.vkinternship2019.domain.PrepareSongListener
import ru.rain.ifmo.vkinternship2019.pair2folder
import ru.rain.ifmo.vkinternship2019.screen.fragment.MiniPlayerFragment
import java.io.File

class MainActivity : AppCompatActivity(), PrepareSongListener {

    companion object {
        private const val PERMISSION_WRITE_EXTERNAL = 101
        private const val REQUEST_PICK_FOLDER = 102
    }

    private val songSingleton = SongSingleton.instance

    private var spinnerDialog = SpinnerDialog().apply { isCancelable = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CYCLE", "onCreate")
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        choose_folder_btn.setOnClickListener{ chooseFolder() }
    }

    override fun onStart() {
        super.onStart()
        songSingleton.prepareSongListener = this
    }

    override fun onPause() {
        super.onPause()
        if (spinnerDialog.isAdded) {
            spinnerDialog.dismiss()
        }
        songSingleton.prepareSongListener = null
    }

    private fun chooseFolder() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_WRITE_EXTERNAL
            )
        } else {
            pickFolder()
        }
    }

    @Suppress("Deprecation")
    private fun pickFolder() {
        val contentResolver = contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )
        val list = arrayListOf<Pair<String, Uri>>()
        cursor?.use {
            if (!cursor.moveToFirst()) {
                Toast.makeText(this@MainActivity, "No Music on sdcard", Toast.LENGTH_SHORT).show()
            } else {
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val pathColId = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                do {
                    val fileUri = ContentUris.withAppendedId(uri, cursor.getLong(idCol))
                    val path = cursor.getString(pathColId)
                    val parentPath = File(path).parentFile?.absolutePath
                    if (parentPath != null) {
                        list.add(Pair(parentPath, fileUri))
                    }
                } while (cursor.moveToNext())
            }
        }
        list.sortWith(Comparator { p0, p1 ->
            p0.first.compareTo(p1.first)
        })
        startActivityForResult(
            FolderListActivity.createIntent(this, list.pair2folder()),
            REQUEST_PICK_FOLDER
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_WRITE_EXTERNAL) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_DENIED) {
                pickFolder()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            songSingleton.mapFolderToPlaylist(applicationContext)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun startPreparing() {
        if (!spinnerDialog.isAdded) {
            spinnerDialog.show(supportFragmentManager, "TAG")
        }
    }

    override fun finishPreparing() {
        spinnerDialog.dismiss()
        //THAT'S OK, PREPARING FINISHED
        if (songSingleton.playList.isNotEmpty()) {
            swapPlayers()
        }
    }

    private fun swapPlayers() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            //No fragments -> show mini player
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, MiniPlayerFragment())
                .commit()
        } else {
            when (fragment) {
                is MiniPlayerFragment -> {

                }
                else -> {

                }
            }
        }
    }
}
