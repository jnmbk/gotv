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

    @Test
    fun initialization_usesLastPlayedUrl_whenValid() = runTest {
        val multiChannel = Channel(
            id = "c-multi",
            name = "Sports",
            streamUrls = listOf("https://s1.test", "https://s2.test", "https://s3.test"),
            lastPlayedUrl = "https://s2.test"
        )
        val vm = PlayerViewModel(multiChannel, engine, network, policy)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasFailed)
        assertEquals("https://s2.test", engine.currentStreamUrl)
        assertEquals(listOf("https://s2.test"), engine.playedUrls)
    }

    @Test
    fun initialization_fallsBackToFirstUrl_whenLastPlayedUrlInvalidOrNull() = runTest {
        val multiChannel = Channel(
            id = "c-multi",
            name = "Sports",
            streamUrls = listOf("https://s1.test", "https://s2.test"),
            lastPlayedUrl = "https://nonexistent.test"
        )
        val vm = PlayerViewModel(multiChannel, engine, network, policy)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasFailed)
        assertEquals("https://s1.test", engine.currentStreamUrl)
        assertEquals(listOf("https://s1.test"), engine.playedUrls)
    }

    @Test
    fun playbackError_cyclesThroughBackupStreamUrls_andPersistsWorkingUrl() = runTest {
        val repo = com.gotv.player.data.repository.InMemoryChannelRepository()
        val multiChannel = Channel(
            id = "c-multi",
            name = "Sports",
            streamUrls = listOf("https://s1.test", "https://s2.test", "https://s3.test")
        )
        repo.addChannel(multiChannel)

        // Engine plays s1 initially
        val vm = PlayerViewModel(multiChannel, engine, network, policy, channelRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://s1.test", engine.currentStreamUrl)

        // Now s1 encounters error, s2 will fail, s3 will succeed
        engine.shouldFailUrl = { url -> url == "https://s1.test" || url == "https://s2.test" }
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "stream broke"))
        dispatcher.scheduler.runCurrent()
        assertEquals(ReconnectPhase.RECONNECTING, vm.uiState.value.reconnectPhase)

        // Advance through reconnection
        dispatcher.scheduler.advanceUntilIdle()

        // Should have cycled: s1 -> s2 (failed) -> s3 (succeeded)
        assertEquals(listOf("https://s1.test", "https://s2.test", "https://s3.test"), engine.playedUrls)
        assertEquals("https://s3.test", engine.currentStreamUrl)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertEquals(ReconnectPhase.NONE, vm.uiState.value.reconnectPhase)

        // Successfully playing backup persists lastPlayedUrl in repo
        val saved = repo.getChannelById("c-multi")
        assertEquals("https://s3.test", saved?.lastPlayedUrl)
    }

    @Test
    fun playbackError_allBackupUrlsFail_entersFailedState_andRetryRestartsCycle() = runTest {
        val repo = com.gotv.player.data.repository.InMemoryChannelRepository()
        val multiChannel = Channel(
            id = "c-multi",
            name = "Sports",
            streamUrls = listOf("https://s1.test", "https://s2.test")
        )
        repo.addChannel(multiChannel)

        val vm = PlayerViewModel(multiChannel, engine, network, policy, channelRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://s1.test", engine.currentStreamUrl)

        // Both s1 and s2 will fail
        engine.failPlays = true
        engine.updatePlaybackState(PlaybackState(PlaybackStatus.ERROR, "stream broke"))
        dispatcher.scheduler.advanceUntilIdle()

        // s1 was initial, s2 was tried as backup URL, both failed
        assertEquals(listOf("https://s1.test", "https://s2.test"), engine.playedUrls)
        assertTrue(vm.uiState.value.hasFailed)

        // User taps retry: retry starts cycle again, with s1 succeeding this time
        engine.failPlays = false
        vm.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasFailed)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertEquals("https://s1.test", engine.currentStreamUrl)
        assertEquals(
            listOf("https://s1.test", "https://s2.test", "https://s1.test"),
            engine.playedUrls
        )
    }
}
