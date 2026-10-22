package com.gotv.player.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gotv.player.data.model.Channel
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
}
