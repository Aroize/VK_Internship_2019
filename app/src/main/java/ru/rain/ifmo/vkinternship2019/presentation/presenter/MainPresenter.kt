package ru.rain.ifmo.vkinternship2019.presentation.presenter

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import ru.rain.ifmo.vkinternship2019.data.song.SongSingleton
import ru.rain.ifmo.vkinternship2019.domain.PrepareSongListener
import ru.rain.ifmo.vkinternship2019.domain.mvp.BasePresenter
import ru.rain.ifmo.vkinternship2019.pair2folder
import java.io.File

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 24.11.2019
 */
class MainPresenter(private val context: Context): BasePresenter<MainView>() {

    enum class Player {
        MINI,
        MAIN,
        EMPTY
    }

    private val songSingleton = SongSingleton.instance

    private var playerState: Player = Player.EMPTY

    @Suppress("Deprecation")
    fun pickFolder(contentResolver: ContentResolver) {
        Thread{
            viewState?.showSpinner()
            showSpinner = true
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
                        val file = File(path)
                        val parentPath = file.parentFile?.absolutePath
                        if (parentPath != null && file.extension == "mp3") {
                            list.add(Pair(parentPath, fileUri))
                        }
                    } while (cursor.moveToNext())
                }
            }
            list.sortBy { it.first }
            val folderList = list.pair2folder()
            viewState?.dismissSpinner()
            showSpinner = false
            viewState?.openExplorer(folderList)
        }.start()
    }

    fun loadSongs() {
        songSingleton.prepareSongListener = object : PrepareSongListener {
            override fun startPreparing() {
                viewState?.showSpinner()
               showSpinner = true
            }

            override fun finishPreparing() {
                viewState?.dismissSpinner()
                setPlayer(if (songSingleton.playList.isEmpty()) Player.EMPTY else Player.MINI)
                showSpinner = false
            }
        }
        songSingleton.mapFolderToPlaylist(context)
    }

    fun setPlayer(player: Player) {
        playerState = player
        when (player) {
            Player.MINI -> {
                viewState?.showMiniPlayer()
            }
            Player.MAIN -> {
                viewState?.showMainPlayer()
            }
            Player.EMPTY -> {
                viewState?.hidePlayer()
            }
        }
    }

    fun swapPlayer() {
        when (playerState) {
            Player.MINI -> setPlayer(Player.MAIN)
            Player.MAIN -> setPlayer(Player.MINI)
            else -> setPlayer(Player.EMPTY)
        }
    }
}