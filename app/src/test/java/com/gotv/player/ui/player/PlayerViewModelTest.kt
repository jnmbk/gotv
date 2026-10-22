package com.gotv.player.ui.player

import com.gotv.player.data.model.Channel
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.player.PlaybackState
import com.gotv.player.player.PlaybackStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeEngine = FakePlayerEngine()
    private val testChannel = Channel(
        id = "ch-1",
        name = "EuroNews Live",
        streamUrl = "https://example.com/euronews/live.m3u8"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_startsPlaybackAutomatically() = runTest {
        val viewModel = PlayerViewModel(testChannel, fakeEngine)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testChannel.streamUrl, fakeEngine.currentStreamUrl)
        assertEquals(PlaybackStatus.PLAYING, fakeEngine.playbackState.value.status)
        assertEquals(testChannel, viewModel.uiState.value.channel)
    }

    @Test
    fun setVolume_delegatesToEngine() = runTest {
        val viewModel = PlayerViewModel(testChannel, fakeEngine)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setVolume(0.42f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.42f, fakeEngine.volume.value)
        assertEquals(0.42f, viewModel.uiState.value.volume)
    }

    @Test
    fun release_releasesEngineResources() = runTest {
        val viewModel = PlayerViewModel(testChannel, fakeEngine)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.release()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeEngine.isReleased)
        assertEquals(1, fakeEngine.releaseCount)
    }

    @Test
    fun playbackStateUpdates_flowToUiState() = runTest {
        val viewModel = PlayerViewModel(testChannel, fakeEngine)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeEngine.updatePlaybackState(PlaybackState(PlaybackStatus.BUFFERING))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PlaybackStatus.BUFFERING, viewModel.uiState.value.playbackState.status)
    }
}
