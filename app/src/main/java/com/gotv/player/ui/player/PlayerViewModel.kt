package com.gotv.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gotv.player.data.model.Channel
import com.gotv.player.network.AlwaysOnlineNetworkMonitor
import com.gotv.player.network.NetworkMonitor
import com.gotv.player.player.PlaybackStatus
import com.gotv.player.player.PlayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Tuning for automatic stream recovery. */
data class ReconnectPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 1_000,
    val offlineTimeoutMs: Long = 30_000,
    val attemptTimeoutMs: Long = 15_000
)

class PlayerViewModel(
    val channel: Channel,
    private val playerEngine: PlayerEngine,
    private val networkMonitor: NetworkMonitor = AlwaysOnlineNetworkMonitor,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy()
) : ViewModel() {

    private val reconnectPhase = MutableStateFlow(ReconnectPhase.NONE)
    private var reconnectJob: Job? = null

    val uiState: StateFlow<PlayerUiState> = combine(
        playerEngine.playbackState,
        playerEngine.volume,
        networkMonitor.isOnline,
        reconnectPhase
    ) { playback, volume, online, phase ->
        PlayerUiState(
            channel = channel,
            playbackState = playback,
            volume = volume,
            isOnline = online,
            reconnectPhase = phase
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerUiState(channel = channel)
    )

    init {
        playerEngine.play(channel.streamUrl)
        viewModelScope.launch {
            playerEngine.playbackState.collect { state ->
                if (state.status == PlaybackStatus.ERROR) startReconnect(immediate = false)
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.drop(1).collect { online ->
                if (online) onNetworkRestored()
            }
        }
    }

    /** Manually restarts recovery after a permanent failure. */
    fun retry() {
        startReconnect(immediate = true)
    }

    private fun onNetworkRestored() {
        if (stoppedInBackground || playerEngine.isReleased) return
        val healthy = reconnectPhase.value == ReconnectPhase.NONE &&
            playerEngine.playbackState.value.status == PlaybackStatus.PLAYING
        if (healthy) return
        reconnectJob?.cancel()
        startReconnect(immediate = true)
    }

    private fun startReconnect(immediate: Boolean) {
        if (stoppedInBackground || playerEngine.isReleased) return
        if (reconnectJob?.isActive == true) return
        reconnectPhase.value = ReconnectPhase.RECONNECTING
        reconnectJob = viewModelScope.launch {
            var attempt = 0
            while (attempt < reconnectPolicy.maxAttempts) {
                if (!(immediate && attempt == 0)) {
                    delay(reconnectPolicy.baseDelayMs shl attempt)
                }
                if (!networkMonitor.isOnline.value) {
                    val back = withTimeoutOrNull(reconnectPolicy.offlineTimeoutMs) {
                        networkMonitor.isOnline.first { it }
                    }
                    if (back == null) break
                }
                attempt++
                playerEngine.play(channel.streamUrl)
                val outcome = withTimeoutOrNull(reconnectPolicy.attemptTimeoutMs) {
                    playerEngine.playbackState.first {
                        it.status == PlaybackStatus.PLAYING || it.status == PlaybackStatus.ERROR
                    }
                }
                if (outcome?.status == PlaybackStatus.PLAYING) {
                    reconnectPhase.value = ReconnectPhase.NONE
                    return@launch
                }
            }
            reconnectPhase.value = ReconnectPhase.FAILED
        }
    }

    fun setVolume(volume: Float) {
        playerEngine.setVolume(volume)
    }

    private var stoppedInBackground = false

    /** Frees decoders and stops audio while the app is not visible. */
    fun onBackground() {
        if (playerEngine.isReleased || stoppedInBackground) return
        stoppedInBackground = true
        reconnectJob?.cancel()
        reconnectPhase.value = ReconnectPhase.NONE
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
        private val playerEngine: PlayerEngine,
        private val networkMonitor: NetworkMonitor = AlwaysOnlineNetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(channel, playerEngine, networkMonitor) as T
        }
    }
}
