package com.gotv.player.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlayerEngine(
    initialVolume: Float = 1.0f,
    initialStatus: PlaybackStatus = PlaybackStatus.IDLE
) : PlayerEngine {

    private val _playbackState = MutableStateFlow(PlaybackState(initialStatus))
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _volume = MutableStateFlow(initialVolume.coerceIn(0f, 1f))
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private var _isReleased: Boolean = false
    override val isReleased: Boolean
        get() = _isReleased

    private var _isStopped: Boolean = false
    val isStopped: Boolean
        get() = _isStopped

    var currentStreamUrl: String? = null
        private set

    var playCount: Int = 0
        private set

    val playedUrls = mutableListOf<String>()

    var stopCount: Int = 0
        private set

    var releaseCount: Int = 0
        private set

    /** When true, [play] simulates an unreachable stream by entering ERROR instead of PLAYING. */
    var failPlays: Boolean = false

    /** Optional predicate to selectively fail specific URLs during [play]. */
    var shouldFailUrl: ((String) -> Boolean)? = null

    override val player: Player? = null

    override fun play(streamUrl: String) {
        currentStreamUrl = streamUrl
        playedUrls.add(streamUrl)
        playCount++
        _isStopped = false
        _isReleased = false
        val isError = shouldFailUrl?.invoke(streamUrl) ?: failPlays
        _playbackState.value =
            if (isError) PlaybackState(PlaybackStatus.ERROR, "Stream unreachable")
            else PlaybackState(PlaybackStatus.PLAYING)
    }

    override fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
    }

    override fun stop() {
        if (!_isStopped) {
            _isStopped = true
            stopCount++
            _playbackState.value = PlaybackState(PlaybackStatus.IDLE)
        }
    }

    override fun release() {
        if (!_isReleased) {
            _isReleased = true
            releaseCount++
            _playbackState.value = PlaybackState(PlaybackStatus.IDLE)
        }
    }

    fun updatePlaybackState(state: PlaybackState) {
        _playbackState.value = state
    }
}
