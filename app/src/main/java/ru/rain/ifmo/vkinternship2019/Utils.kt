package ru.rain.ifmo.vkinternship2019

import android.net.Uri
import ru.rain.ifmo.vkinternship2019.data.filesystem.MusicFolder

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 22.11.2019
 */

fun ArrayList<Pair<String, Uri>>.pair2folder(): ArrayList<MusicFolder> {
    val result = arrayListOf<MusicFolder>()
    val iterator = iterator()
    if (!iterator.hasNext())
        return ArrayList()
    val (a, b) = iterator.next()
    var currentParent = a
    var currentSongs = arrayListOf(b)
    if (!iterator.hasNext())
        return arrayListOf(
            MusicFolder(
                currentParent,
                currentSongs
            )
        )
    do {
        val (newParent, newSong) = iterator.next()
        if (currentParent == newParent) {
            currentSongs.add(newSong)
        } else {
            result.add(
                MusicFolder(
                    currentParent,
                    currentSongs
                )
            )
            currentParent = newParent
            currentSongs = arrayListOf(newSong)
        }
    } while (iterator.hasNext())
    result.add(
        MusicFolder(
            currentParent,
            currentSongs
        )
    )
    return result
}

fun Long.toPlayerDuration(includeDash: Boolean = true): String {
    val sb = StringBuilder()
    if (includeDash)
        sb.append('-')
    val minutes = (this / 60000)
    val seconds = (this / 1000) % 60
    sb.append(minutes.toString())
    sb.append(':')
    if (seconds < 10) {
        sb.append('0')
    }
    sb.append(seconds.toString())
    return sb.toString()
}