package ru.rain.ifmo.vkinternship2019.data.song

import android.graphics.Bitmap
import android.net.Uri

/**
 * @project VK_Internship_2019
 * @author Ilia Ilmenskii created on 22.11.2019
 */
data class Song(
    val albumImage: Bitmap?,
    val name: String,
    val author: String,
    val length: Long,
    val uri: Uri
)