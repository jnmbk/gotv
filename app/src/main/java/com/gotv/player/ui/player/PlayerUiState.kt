package com.gotv.player.ui.player

import com.gotv.player.data.model.Channel
import com.gotv.player.player.PlaybackState
import com.gotv.player.player.PlaybackStatus

data class PlayerUiState(
    val channel: Channel,
    val playbackState: PlaybackState = PlaybackState(PlaybackStatus.BUFFERING),
    val volume: Float = 1.0f
)
