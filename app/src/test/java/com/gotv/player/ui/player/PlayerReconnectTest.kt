package com.gotv.player.ui.player

import com.gotv.player.data.model.Channel
import com.gotv.player.network.FakeNetworkMonitor
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.player.PlaybackState
import com.gotv.player.player.PlaybackStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerReconnectTest {

    private val dispatcher = StandardTestDispatcher()
    private val engine = FakePlayerEngine()
    private val network = FakeNetworkMonitor()
    private val policy = ReconnectPolicy(
        maxAttempts = 3,
        baseDelayMs = 1_000,
        offlineTimeoutMs = 10_000,
        attemptTimeoutMs = 5_000
    )
    private val channel = Channel(id = "ch", name = "News", streamUrl = "https://example.com/live.m3u8")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PlayerViewModel(channel, engine, network, policy)

    @Test
    fun networkDrop_showsSpinnerImmediately_andRestoreResumesPlayback() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.showSpinner)

        network.setOnline(false)
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.BUFFERING))
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.showSpinner)

        network.setOnline(true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, engine.playCount)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertFalse(vm.uiState.value.showSpinner)
        assertFalse(vm.uiState.value.hasFailed)
    }

    @Test
    fun offlineWhilePlaying_showsSpinner_untilOnlineAgain() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        network.setOnline(false)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.showSpinner)

        network.setOnline(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.showSpinner)
    }

    @Test
    fun streamError_retriesAndRecovers() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "boom"))
        dispatcher.scheduler.runCurrent()
        assertEquals(ReconnectPhase.RECONNECTING, vm.uiState.value.reconnectPhase)
        assertTrue(vm.uiState.value.showSpinner)

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertEquals(ReconnectPhase.NONE, vm.uiState.value.reconnectPhase)
    }

    @Test
    fun permanentlyBrokenStream_failsAfterRetries_andRetryRestarts() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        engine.failPlays = true
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "boom"))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.hasFailed)
        assertFalse(vm.uiState.value.showSpinner)
        assertEquals(1 + policy.maxAttempts, engine.playCount)

        engine.failPlays = false
        vm.retry()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.hasFailed)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
    }

    @Test
    fun persistentNetworkLoss_failsAfterOfflineTimeout() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        network.setOnline(false)
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "offline"))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.hasFailed)
    }

    @Test
    fun networkRestoredAfterFailure_resumesAutomatically() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        network.setOnline(false)
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "offline"))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.hasFailed)

        network.setOnline(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.hasFailed)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
    }

    @Test
    fun background_cancelsReconnect() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        engine.failPlays = true
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "boom"))
        dispatcher.scheduler.runCurrent()

        vm.onBackground()
        val plays = engine.playCount
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(plays, engine.playCount)
        assertEquals(ReconnectPhase.NONE, vm.uiState.value.reconnectPhase)
    }
}
