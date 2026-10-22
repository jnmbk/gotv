package com.gotv.player.ui.channel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.data.repository.SqliteChannelRepository
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelGridScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displayed_whenNoChannelsAddedYet() {
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("empty_channels_view").assertIsDisplayed()
        composeTestRule.onNodeWithText("No channels yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_add_channel_button").assertIsDisplayed()
    }

    @Test
    fun tappingAddButton_opensAddChannelDialog() {
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Tap empty state Add Channel button
        composeTestRule.onNodeWithTag("empty_add_channel_button").performClick()

        // Dialog should be displayed with input fields
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stream_url_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("logo_url_input").assertIsDisplayed()
    }

    @Test
    fun inputValidation_showsClearFeedback_onEmptyNameOrInvalidUrl() {
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Open dialog from top bar action
        composeTestRule.onNodeWithTag("add_channel_top_bar_button").performClick()
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()

        // Input invalid values
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("invalid-stream-link")
        composeTestRule.onNodeWithTag("save_channel_button").performClick()

        // Validation messages should guide the user
        composeTestRule.onNodeWithText("Channel name cannot be empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please enter a valid M3U8 URL (e.g. https://example.com/live.m3u8)").assertIsDisplayed()
        // Dialog remains open
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()
    }

    @Test
    fun savingValidChannel_addsChannelToGrid_immediatelyWithPlaceholderLogo() {
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Open dialog and enter valid channel info
        composeTestRule.onNodeWithTag("empty_add_channel_button").performClick()
        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("EuroNews")
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://streams.euronews.com/live.m3u8")
        composeTestRule.onNodeWithTag("save_channel_button").performClick()

        // Grid should immediately display the channel card with name and placeholder logo
        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("EuroNews").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_logo_placeholder", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("E", useUnmergedTree = true).assertExists()
    }

    @Test
    fun multipleChannels_displayedInGrid() {
        val repository = InMemoryChannelRepository(
            listOf(
                Channel(name = "Sports 1", streamUrl = "https://example.com/sports1.m3u8"),
                Channel(name = "Cinema HD", streamUrl = "https://example.com/cinema.m3u8")
            )
        )
        val viewModel = ChannelGridViewModel(repository)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sports 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cinema HD").assertIsDisplayed()
    }

    @Test
    fun storedChannels_persistAndDisplayAcrossAppRestarts() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "restart_test.db"

        // Session 1: Channel is saved to SQLite persistent storage
        val repoSession1 = SqliteChannelRepository(context, dbName)
        kotlinx.coroutines.runBlocking {
            repoSession1.addChannel(
                Channel(
                    name = "Persistent Channel",
                    streamUrl = "https://persist.example.com/live.m3u8"
                )
            )
        }

        // Session 2: Fresh app start with new repository and view model instance reading from SQLite
        val repoSession2 = SqliteChannelRepository(context, dbName)
        val viewModelSession2 = ChannelGridViewModel(repoSession2)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModelSession2)
            }
        }

        // Stored channel persists and displays correctly across restarts
        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Persistent Channel").assertIsDisplayed()
    }
}

