package com.gotv.player.ui.player

import com.gotv.player.data.model.Channel
import com.gotv.player.player.PlaybackState
import com.gotv.player.player.PlaybackStatus

/** Progress of automatic recovery after a network drop or stream error. */
enum class ReconnectPhase {
    NONE,
    RECONNECTING,
    FAILED
}

data class PlayerUiState(
    val channel: Channel,
    val playbackState: PlaybackState = PlaybackState(PlaybackStatus.BUFFERING),
    val volume: Float = 1.0f,
    val isOnline: Boolean = true,
    val reconnectPhase: ReconnectPhase = ReconnectPhase.NONE
) {
    val hasFailed: Boolean
        get() = reconnectPhase == ReconnectPhase.FAILED

    /** True while the stream is buffering, reconnecting, or the network is down. */
    val showSpinner: Boolean
        get() = !hasFailed && (
            reconnectPhase == ReconnectPhase.RECONNECTING ||
                !isOnline ||
                playbackState.status == PlaybackStatus.BUFFERING ||
                playbackState.status == PlaybackStatus.ERROR
            )
}
