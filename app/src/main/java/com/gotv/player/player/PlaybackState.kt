package com.gotv.player.player

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    ERROR
}

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val errorMessage: String? = null
)
