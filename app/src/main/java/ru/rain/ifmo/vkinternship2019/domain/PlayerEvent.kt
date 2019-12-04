package ru.rain.ifmo.vkinternship2019.domain

enum class PlayerEvent {
    PLAY_PAUSE,
    SEEK,
    NEXT,
    PREV,
    NEW_PLAYLIST;

    fun toInt(): Int =
        when (this) {
            PLAY_PAUSE -> { 0 }
            SEEK -> { 1 }
            NEXT -> { 2 }
            PREV -> { 3 }
            NEW_PLAYLIST -> { 4 }
        }
}
