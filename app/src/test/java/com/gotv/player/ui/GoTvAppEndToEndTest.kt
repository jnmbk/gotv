package com.gotv.player.ui

import com.gotv.player.testing.SlowTest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.player.PlaybackStatus
import com.gotv.player.ui.theme.GoTvTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class GoTvAppEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleChannel = Channel(
        id = "test-live-channel-1",
        name = "Bloomberg HD",
        streamUrl = "https://live.bloomberg.com/stream.m3u8"
    )

    @Test
    fun gridToPlayerNavigation_startsLivePlayback_andExitReturnsToGridReleasingPlayer() = runBlocking {
        val channelRepository = InMemoryChannelRepository()
        channelRepository.addChannel(sampleChannel)

        var createdPlayerEngine: FakePlayerEngine? = null

        composeTestRule.setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = channelRepository,
                    logoStorage = null,
                    playerEngineFactory = {
                        FakePlayerEngine().also { createdPlayerEngine = it }
                    }
                )
            }
        }

        // 1. Grid screen is visible with Bloomberg channel
        composeTestRule.onNodeWithTag("channel_grid_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bloomberg HD").assertIsDisplayed()

        // 2. Tapping the channel tile navigates to dedicated full-screen live player
        composeTestRule.onNodeWithText("Bloomberg HD").performClick()

        // 3. Player screen is active and displays channel title & video surface
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_channel_title").assertTextEquals("Bloomberg HD")

        // 4. M3U8 stream began playing automatically with media engine
        val engine = requireNotNull(createdPlayerEngine) { "Player engine should have been instantiated" }
        assertEquals(sampleChannel.streamUrl, engine.currentStreamUrl)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertFalse(engine.isReleased)

        // 5. Volume control is functional
        composeTestRule.onNodeWithTag("volume_slider").assertIsDisplayed()
        composeTestRule.onNodeWithTag("volume_icon_button").performClick() // Mute
        assertEquals(0.0f, engine.volume.value, 0.001f)

        // 6. Navigating back returns to grid and immediately frees media engine resources
        composeTestRule.onNodeWithTag("player_back_button").performClick()

        // Player screen is gone, grid screen is back
        composeTestRule.onNodeWithTag("player_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("channel_grid_screen").assertIsDisplayed()

        // Media engine and decoder resources are immediately released
        assertTrue("Player engine must be released upon navigating back to grid", engine.isReleased)
        assertEquals(1, engine.releaseCount)
    }

    @Test
    fun volume_isPreservedAcrossChannelChanges() = runBlocking {
        val channel1 = Channel(
            id = "test-live-channel-1",
            name = "Bloomberg HD",
            streamUrl = "https://live.bloomberg.com/stream.m3u8"
        )
        val channel2 = Channel(
            id = "test-live-channel-2",
            name = "CNN HD",
            streamUrl = "https://live.cnn.com/stream.m3u8"
        )
        val channelRepository = InMemoryChannelRepository()
        channelRepository.addChannel(channel1)
        channelRepository.addChannel(channel2)

        val createdEngines = mutableListOf<FakePlayerEngine>()

        composeTestRule.setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = channelRepository,
                    logoStorage = null,
                    playerEngineFactory = {
                        FakePlayerEngine().also { createdEngines.add(it) }
                    }
                )
            }
        }

        // Open channel 1
        composeTestRule.onNodeWithText("Bloomberg HD").performClick()
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
        val engine1 = createdEngines.first()
        assertEquals(1.0f, engine1.volume.value, 0.001f)

        // Mute volume on channel 1 (volume becomes 0.0f)
        composeTestRule.onNodeWithTag("volume_icon_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0.0f, engine1.volume.value, 0.001f)

        // Go back to grid
        composeTestRule.onNodeWithTag("player_back_button").performClick()
        composeTestRule.onNodeWithTag("channel_grid_screen").assertIsDisplayed()

        // Open channel 2
        composeTestRule.onNodeWithText("CNN HD").performClick()
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
        val engine2 = createdEngines.last()

        // Volume should remain at 0.0f, not reset to 1.0f
        assertEquals(0.0f, engine2.volume.value, 0.001f)

        // Switch back to channel 1 via the sidebar
        composeTestRule.onNodeWithTag("video_viewport").performClick()
        composeTestRule.onNodeWithTag("sidebar_channel_test-live-channel-1").performClick()
        composeTestRule.waitForIdle()

        val engine3 = createdEngines.last()
        assertEquals(0.0f, engine3.volume.value, 0.001f)
    }
}
