package com.gotv.player.ui.channel

import android.graphics.Bitmap
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
import com.gotv.player.data.storage.FileLogoStorage
import com.gotv.player.data.storage.ImageDownloader
import com.gotv.player.data.storage.InMemoryLogoStorage
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ChannelGridScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSamplePngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

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

    @Test
    fun savingChannelWithValidLogoUrl_downloadsCachesAndDisplaysLogoImage() {
        val sampleBytes = createSamplePngBytes()
        var downloadCalls = 0
        val fakeDownloader = ImageDownloader {
            downloadCalls++
            sampleBytes
        }
        val storage = FileLogoStorage(
            baseDir = tempFolder.newFolder("logos_e2e_valid"),
            imageDownloader = fakeDownloader
        )
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository, storage)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Open dialog and enter valid channel info with logo URL
        composeTestRule.onNodeWithTag("empty_add_channel_button").performClick()
        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("France 24")
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://streams.example.com/f24.m3u8")
        composeTestRule.onNodeWithTag("logo_url_input").performTextInput("https://example.com/f24.png")
        composeTestRule.onNodeWithTag("save_channel_button").performClick()

        // Wait for composition to settle
        composeTestRule.waitForIdle()

        // Grid should display channel tile and cached logo image
        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("France 24").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_logo_image", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(1, downloadCalls)
    }

    @Test
    fun savingChannelWithFailingLogoUrl_fallsBackGracefullyToPlaceholder() {
        var downloadCalls = 0
        val failingDownloader = ImageDownloader {
            downloadCalls++
            null
        }
        val storage = FileLogoStorage(
            baseDir = tempFolder.newFolder("logos_e2e_fail"),
            imageDownloader = failingDownloader
        )
        val repository = InMemoryChannelRepository()
        val viewModel = ChannelGridViewModel(repository, storage)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Open dialog and enter channel info with failing logo URL
        composeTestRule.onNodeWithTag("empty_add_channel_button").performClick()
        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("BBC News")
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://streams.example.com/bbc.m3u8")
        composeTestRule.onNodeWithTag("logo_url_input").performTextInput("https://unreachable.example.com/bad.png")
        composeTestRule.onNodeWithTag("save_channel_button").performClick()

        composeTestRule.waitForIdle()

        // Grid displays channel with placeholder fallback without crashing
        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("BBC News").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_logo_placeholder", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("B", useUnmergedTree = true).assertExists()
        assertEquals(1, downloadCalls)
    }

    @Test
    fun storedChannelWithCachedLogo_persistsAcrossAppRestarts_andRendersWithoutRedownloading() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "restart_logo_test.db"
        val logoFolder = tempFolder.newFolder("logos_restart")

        // Pre-create a cached downsampled logo file on disk
        val cachedFile = File(logoFolder, "persisted-chan.png")
        val sampleBytes = createSamplePngBytes()
        FileOutputStream(cachedFile).use { it.write(sampleBytes) }
        assertTrue(cachedFile.exists())

        var networkCalls = 0
        val fakeDownloader = ImageDownloader {
            networkCalls++
            sampleBytes
        }
        val storage = FileLogoStorage(baseDir = logoFolder, imageDownloader = fakeDownloader)

        // Session 1: Persist channel with localLogoPath pointing to the cached file
        val repoSession1 = SqliteChannelRepository(context, dbName)
        kotlinx.coroutines.runBlocking {
            repoSession1.addChannel(
                Channel(
                    id = "persisted-chan",
                    name = "Cached News",
                    streamUrl = "https://persist.example.com/live.m3u8",
                    logoUrl = "https://example.com/logo.png",
                    localLogoPath = cachedFile.absolutePath
                )
            )
        }

        // Session 2: Cold start with new repository and viewmodel instance
        val repoSession2 = SqliteChannelRepository(context, dbName)
        val viewModelSession2 = ChannelGridViewModel(repoSession2, storage)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModelSession2)
            }
        }

        // Verify the cached logo image renders locally from disk without re-downloading
        composeTestRule.onNodeWithTag("channels_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cached News").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_logo_image", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, networkCalls)
    }
}

