package com.gotv.player.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

/**
 * Lightweight abstraction over the media playback engine.
 * Allows decoupling Compose UI and ViewModels from ExoPlayer specifics
 * and enables hermetic testing with test doubles.
 */
interface PlayerEngine {
    val playbackState: StateFlow<PlaybackState>
    val volume: StateFlow<Float>
    val isReleased: Boolean
    val player: Player?

    fun play(streamUrl: String)
    fun setVolume(volume: Float)
    fun stop()
    fun release()
}
