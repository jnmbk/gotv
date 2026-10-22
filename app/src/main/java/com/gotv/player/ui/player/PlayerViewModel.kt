package com.gotv.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gotv.player.data.model.Channel
import com.gotv.player.player.PlayerEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(
    val channel: Channel,
    private val playerEngine: PlayerEngine
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = combine(
        playerEngine.playbackState,
        playerEngine.volume
    ) { playback, volume ->
        PlayerUiState(
            channel = channel,
            playbackState = playback,
            volume = volume
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerUiState(channel = channel)
    )

    init {
        playerEngine.play(channel.streamUrl)
    }

    fun setVolume(volume: Float) {
        playerEngine.setVolume(volume)
    }

    private var stoppedInBackground = false

    /** Frees decoders and stops audio while the app is not visible. */
    fun onBackground() {
        if (playerEngine.isReleased || stoppedInBackground) return
        stoppedInBackground = true
        playerEngine.stop()
    }

    /** Restarts the live stream if it was stopped by [onBackground]. */
    fun onForeground() {
        if (!stoppedInBackground || playerEngine.isReleased) return
        stoppedInBackground = false
        playerEngine.play(channel.streamUrl)
    }

    fun release() {
        playerEngine.release()
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }

    class Factory(
        private val channel: Channel,
        private val playerEngine: PlayerEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(channel, playerEngine) as T
        }
    }
}
