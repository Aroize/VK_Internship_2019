package ru.rain.ifmo.vkinternship2019.data.song

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import ru.rain.ifmo.vkinternship2019.R
import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder
import ru.rain.ifmo.vkinternship2019.domain.PrepareSongListener

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 22.11.2019
 */

class SongSingleton private constructor() {

    companion object {
        private var singleton: SongSingleton? = null

        val instance: SongSingleton
            get() {
                if (singleton == null) {
                    singleton =
                        SongSingleton()
                }
                return singleton as SongSingleton
            }
    }

    private val handlerCallback = Handler.Callback {
        prepareSongListener?.finishPreparing()
        isRunning = false
        true
    }

    private val handler = Handler(handlerCallback)

    private val retriever = MediaMetadataRetriever()

    lateinit var storage: MusicFolder

    val playList = arrayListOf<Song>()

    private var _index = 0
    val index: Int
    get() = _index

    fun currentSong(): Song {
        while (playList.size == 0) {}
        return playList[_index]
    }

    fun nextSong(): Song {
        _index = (_index + 1) % playList.size
        return playList[_index]
    }

    fun prevSong(): Song {
        _index--
        if (_index < 0)
            _index = playList.size - 1
        return playList[_index]
    }

    private var isRunning = false

    var prepareSongListener: PrepareSongListener? = null
    set(value) {
        field = value
        if (isRunning) {
            prepareSongListener?.startPreparing()
        }
    }

    fun mapFolderToPlaylist(context: Context) {
        if (this::storage.isInitialized) {
            Thread {
                isRunning = true
                _index = 0
                val undefined = context.getString(R.string.undefined)
                playList.clear()
                prepareSongListener?.startPreparing()
                val uris = storage.songs
                uris.forEach {
                    retriever.setDataSource(context, it)
                    val byteArray = retriever.embeddedPicture
                    var bitmap: Bitmap? = null
                    if (byteArray != null) {
                        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    }
                    val name = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val length =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    playList.add(
                        Song(
                            bitmap,
                            name ?: undefined,
                            artist ?: undefined,
                            length.toLong(),
                            it
                        )
                    )
                }
                handler.sendEmptyMessage(0)
            }.start()
        }
    }
}