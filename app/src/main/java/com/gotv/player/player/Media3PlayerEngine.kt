package com.gotv.player.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AndroidX Media3 (ExoPlayer) implementation strictly configured for low-RAM devices:
 * - Constrained buffer durations (1.5s - 4s) to prevent memory spikes on low-spec hardware.
 * - Back buffering and DVR seek cache disabled.
 * - Codec instances and hardware decoders immediately freed upon stop or release.
 */
/**
 * Maps an ExoPlayer state to a [PlaybackState]. ExoPlayer drops to STATE_IDLE after a fatal
 * error, so an existing ERROR is kept on IDLE until playback is restarted (BUFFERING/READY).
 */
internal fun reducePlaybackState(current: PlaybackState, exoPlayerState: Int): PlaybackState =
    when (exoPlayerState) {
        Player.STATE_BUFFERING -> PlaybackState(PlaybackStatus.BUFFERING)
        Player.STATE_READY -> PlaybackState(PlaybackStatus.PLAYING)
        else ->
            if (current.status == PlaybackStatus.ERROR) current
            else PlaybackState(PlaybackStatus.IDLE)
    }

class Media3PlayerEngine(
    context: Context,
    private val exoPlayer: ExoPlayer = createLowRamExoPlayer(context)
) : PlayerEngine {

    private val _playbackState = MutableStateFlow(PlaybackState(PlaybackStatus.IDLE))
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _volume = MutableStateFlow(exoPlayer.volume.coerceIn(0f, 1f))
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private var _isReleased: Boolean = false
    override val isReleased: Boolean
        get() = _isReleased

    override val player: Player
        get() = exoPlayer

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = reducePlaybackState(_playbackState.value, playbackState)
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.value = PlaybackState(
                status = PlaybackStatus.ERROR,
                errorMessage = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
            )
        }

        override fun onVolumeChanged(newVolume: Float) {
            _volume.value = newVolume.coerceIn(0f, 1f)
        }
    }

    init {
        exoPlayer.addListener(listener)
        exoPlayer.playWhenReady = true
    }

    override fun play(streamUrl: String) {
        if (_isReleased) return
        _playbackState.value = PlaybackState(status = PlaybackStatus.BUFFERING)
        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun setVolume(volume: Float) {
        if (_isReleased) return
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _volume.value = clamped
    }

    override fun stop() {
        if (_isReleased) return
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playbackState.value = PlaybackState(status = PlaybackStatus.IDLE)
    }

    override fun release() {
        if (!_isReleased) {
            _isReleased = true
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
            _playbackState.value = PlaybackState(status = PlaybackStatus.IDLE)
        }
    }

    companion object {
        fun createLowRamExoPlayer(context: Context): ExoPlayer {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 1500,
                    /* maxBufferMs = */ 4000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 1000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(0, false)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(8000)
                .setAllowCrossProtocolRedirects(true)

            val hlsMediaSourceFactory = HlsMediaSource.Factory(httpDataSourceFactory)

            return ExoPlayer.Builder(context.applicationContext)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(hlsMediaSourceFactory)
                .build()
        }
    }
}
