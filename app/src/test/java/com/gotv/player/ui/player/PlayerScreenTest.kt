package com.gotv.player.ui.player

import com.gotv.player.testing.SlowTest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gotv.player.data.model.Channel
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testChannel = Channel(
        id = "channel-42",
        name = "France 24 English",
        streamUrl = "https://live.france24.com/hls/live.m3u8"
    )

    @Test
    fun fullScreenPlayer_rendersVideoSurfaceAndTitle_andOmitsSeekAndPauseControls() {
        val fakeEngine = FakePlayerEngine()
        val viewModel = PlayerViewModel(testChannel, fakeEngine)

        composeTestRule.setContent {
            GoTvTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    playerEngine = fakeEngine,
                    onBack = {}
                )
            }
        }

        // Full-screen player container and video surface are displayed
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_surface").assertIsDisplayed()

        // Channel title is displayed
        composeTestRule.onNodeWithTag("player_channel_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_channel_title").assertTextEquals("France 24 English")

        // Volume control is displayed
        composeTestRule.onNodeWithTag("volume_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("volume_icon_button").assertIsDisplayed()

        // Crucial live requirement: Unnecessary pause, seek, and scrubber controls MUST NOT exist
        composeTestRule.onNodeWithTag("seek_bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pause_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("play_pause_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("scrubber").assertDoesNotExist()
    }

    @Test
    fun volumeControl_muteButtonTogglesMuteAndUnmute() {
        val fakeEngine = FakePlayerEngine(initialVolume = 1.0f)
        val viewModel = PlayerViewModel(testChannel, fakeEngine)

        composeTestRule.setContent {
            GoTvTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    playerEngine = fakeEngine,
                    onBack = {}
                )
            }
        }

        assertEquals(1.0f, fakeEngine.volume.value, 0.001f)

        // Click mute button
        composeTestRule.onNodeWithTag("volume_icon_button").performClick()
        assertEquals(0.0f, fakeEngine.volume.value, 0.001f)

        // Click again to restore volume to full
        composeTestRule.onNodeWithTag("volume_icon_button").performClick()
        assertEquals(1.0f, fakeEngine.volume.value, 0.001f)
    }

    @Test
    fun backButton_callsOnBack_andFreesPlayerEngineResources() {
        val fakeEngine = FakePlayerEngine()
        val viewModel = PlayerViewModel(testChannel, fakeEngine)
        var backClicked = false

        composeTestRule.setContent {
            GoTvTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    playerEngine = fakeEngine,
                    onBack = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("player_back_button").performClick()

        assertTrue(backClicked)
        assertTrue(fakeEngine.isReleased)
    }

    /** Runs pending main-looper work (view model coroutines) and advances the frozen compose clock. */
    private fun settle(ms: Long) {
        ShadowLooper.idleMainLooper(ms, TimeUnit.MILLISECONDS)
        composeTestRule.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun networkDropAndReconnect_showsSpinnerThenResumes() {
        val fakeEngine = FakePlayerEngine()
        val network = com.gotv.player.network.FakeNetworkMonitor()
        val viewModel = PlayerViewModel(testChannel, fakeEngine, network)
        // The indeterminate spinner animates forever and would block waitForIdle().
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            GoTvTheme { PlayerScreen(viewModel = viewModel, playerEngine = fakeEngine, onBack = {}) }
        }
        settle(100)
        composeTestRule.onNodeWithTag("loading_spinner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("player_loading_logo").assertDoesNotExist()

        network.setOnline(false)
        fakeEngine.updatePlaybackState(
            com.gotv.player.player.PlaybackState(com.gotv.player.player.PlaybackStatus.BUFFERING)
        )
        settle(100)
        composeTestRule.onNodeWithTag("loading_spinner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_loading_logo").assertIsDisplayed()

        network.setOnline(true)
        settle(100)
        composeTestRule.onNodeWithTag("loading_spinner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("player_loading_logo").assertDoesNotExist()
        assertEquals(2, fakeEngine.playCount)
    }

    @Test
    fun persistentFailure_showsErrorAndAllowsReturningToGrid() {
        val fakeEngine = FakePlayerEngine()
        val network = com.gotv.player.network.FakeNetworkMonitor(initialOnline = false)
        val policy = ReconnectPolicy(maxAttempts = 1, baseDelayMs = 1, offlineTimeoutMs = 1, attemptTimeoutMs = 1)
        val viewModel = PlayerViewModel(testChannel, fakeEngine, network, policy)
        var backCalled = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            GoTvTheme { PlayerScreen(viewModel = viewModel, playerEngine = fakeEngine, onBack = { backCalled = true }) }
        }
        fakeEngine.updatePlaybackState(
            com.gotv.player.player.PlaybackState(com.gotv.player.player.PlaybackStatus.ERROR, "x")
        )
        settle(500)
        assertTrue(composeTestRule.onAllNodesWithTag("playback_error").fetchSemanticsNodes().isNotEmpty())
        composeTestRule.onNodeWithTag("playback_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_loading_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loading_spinner").assertDoesNotExist()

        composeTestRule.onNodeWithTag("error_back_button").performClick()
        assertTrue(backCalled)
    }
}
