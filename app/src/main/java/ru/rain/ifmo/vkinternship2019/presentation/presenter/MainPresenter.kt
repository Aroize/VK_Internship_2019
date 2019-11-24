package ru.rain.ifmo.vkinternship2019.presentation.presenter

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import ru.rain.ifmo.vkinternship2019.data.SongSingleton
import ru.rain.ifmo.vkinternship2019.domain.BasePresenter
import ru.rain.ifmo.vkinternship2019.domain.MainState
import ru.rain.ifmo.vkinternship2019.domain.MvpState
import ru.rain.ifmo.vkinternship2019.domain.PrepareSongListener
import ru.rain.ifmo.vkinternship2019.pair2folder
import java.io.File

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
class MainPresenter: BasePresenter<MainView>() {

    enum class Player {
        MINI,
        MAIN,
        EMPTY
    }

    private val songSingleton = SongSingleton.instance

    private var isSpinnerRunning = false
    private var playerState: Player = Player.EMPTY

    override fun getState(): MvpState {
        val song = if (songSingleton.playList.isNotEmpty())
            songSingleton.currentSong()
        else
            null
        return MainState(isSpinnerRunning, playerState, song)
    }

    @Suppress("Deprecation")
    fun pickFolder(contentResolver: ContentResolver) {
        Thread{
            viewState?.showSpinner()
            isSpinnerRunning = true
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
                if (cursor.moveToFirst()) {
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
            val folderList = list.pair2folder()
            viewState?.dismissSpinner()
            isSpinnerRunning = false
            viewState?.openExplorer(folderList)
        }.start()
    }

    fun loadSongs(context: Context) {
        songSingleton.prepareSongListener = object : PrepareSongListener {
            override fun startPreparing() {
                viewState?.showSpinner()
                isSpinnerRunning = true
            }

            override fun finishPreparing() {
                viewState?.dismissSpinner()
                setPlayer(if (songSingleton.playList.isEmpty()) Player.EMPTY else Player.MINI)
                isSpinnerRunning = false
            }
        }
        songSingleton.mapFolderToPlaylist(context)
    }

    private fun setPlayer(player: Player) {
        playerState = player
        when (player) {
            Player.MINI -> {
                viewState?.showMiniPlayer(songSingleton.currentSong())
            }
            Player.MAIN -> {
                viewState?.showMainPlayer(songSingleton.currentSong())
            }
            Player.EMPTY -> {
                viewState?.hidePlayer()
            }
        }
    }
}