package com.gotv.player.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.testing.SlowTest

import com.gotv.player.ui.player.SIDEBAR_AUTO_DISMISS_MS
import com.gotv.player.ui.theme.GoTvTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class GoTvAppSidebarEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val first = Channel(id = "c1", name = "Bloomberg HD", streamUrl = "https://a.test/1.m3u8", createdAt = 1)
    private val second = Channel(id = "c2", name = "Al Jazeera", streamUrl = "https://a.test/2.m3u8", createdAt = 2)
    private val engines = mutableListOf<FakePlayerEngine>()

    @Before
    fun setUp() {
        val repo = InMemoryChannelRepository()
        runBlocking {
            repo.addChannel(first)
            repo.addChannel(second)
        }
        composeTestRule.setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = repo,
                    playerEngineFactory = { FakePlayerEngine().also { engines.add(it) } }
                )
            }
        }
        composeTestRule.onNodeWithText("Bloomberg HD").performClick()
        composeTestRule.onNodeWithTag("player_screen").assertIsDisplayed()
    }

    private fun tapVideo() {
        composeTestRule.onNodeWithTag("video_viewport").performTouchInput { click(center) }
    }

    @Test
    fun touchOpensSidebarListingChannels() {
        composeTestRule.onNodeWithTag("channel_sidebar").assertDoesNotExist()
        tapVideo()
        composeTestRule.onNodeWithTag("channel_sidebar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sidebar_channel_name_c1", useUnmergedTree = true).assertTextEquals("Bloomberg HD")
        composeTestRule.onNodeWithTag("sidebar_channel_name_c2", useUnmergedTree = true).assertTextEquals("Al Jazeera")
    }

    @Test
    fun sidebarAutoDismissesAfterTimeout() {
        tapVideo()
        composeTestRule.onNodeWithTag("channel_sidebar").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(SIDEBAR_AUTO_DISMISS_MS + 1_000)
        composeTestRule.onNodeWithTag("channel_sidebar").assertDoesNotExist()
    }

    @Test
    fun touchingSidebarResetsCountdown() {
        tapVideo()
        composeTestRule.mainClock.advanceTimeBy(SIDEBAR_AUTO_DISMISS_MS - 2_000)
        composeTestRule.onNodeWithTag("channel_sidebar").performTouchInput { click(center) }
        composeTestRule.mainClock.advanceTimeBy(SIDEBAR_AUTO_DISMISS_MS - 2_000)
        composeTestRule.onNodeWithTag("channel_sidebar").assertIsDisplayed()
        // Step the clock so the dismiss animation gets frames to complete.
        repeat(12) { composeTestRule.mainClock.advanceTimeBy(1_000) }
        composeTestRule.onNodeWithTag("channel_sidebar").assertDoesNotExist()
    }

    @Test
    fun tappingOutsideDismissesSidebar() {
        tapVideo()
        composeTestRule.onNodeWithTag("channel_sidebar").assertIsDisplayed()
        tapVideo()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithTag("channel_sidebar").assertDoesNotExist()
    }

    @Test
    fun selectingChannelSwitchesStreamAndReleasesPrevious() {
        tapVideo()
        composeTestRule.onNodeWithTag("sidebar_channel_c2").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithTag("player_channel_title").assertTextEquals("Al Jazeera")
        assertEquals(2, engines.size)
        assertTrue(engines[0].isReleased)
        assertEquals(second.streamUrl, engines[1].currentStreamUrl)
        composeTestRule.onNodeWithTag("channel_sidebar").assertDoesNotExist()
    }
}
