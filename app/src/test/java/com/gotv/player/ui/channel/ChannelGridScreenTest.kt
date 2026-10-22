package com.gotv.player.ui.channel

import com.gotv.player.testing.SlowTest

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.data.repository.SqliteChannelRepository
import com.gotv.player.data.storage.FileLogoStorage
import com.gotv.player.data.storage.ImageDownloader
import com.gotv.player.data.storage.InMemoryLogoStorage
import com.gotv.player.ui.qr.FakeQrScannerProvider
import com.gotv.player.ui.theme.GoTvTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Category(SlowTest::class)
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
        composeTestRule.onNodeWithText("Please enter a valid stream URL").assertIsDisplayed()
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
            imageDownloader = fakeDownloader,
            ioDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
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

    @Test
    fun longPressChannel_opensContextMenu_withExpectedOptions() {
        val repository = InMemoryChannelRepository(
            listOf(
                Channel(
                    id = "with-logo",
                    name = "With Logo",
                    streamUrl = "https://example.com/1.m3u8",
                    logoUrl = "https://example.com/logo.png"
                ),
                Channel(
                    id = "no-logo",
                    name = "No Logo",
                    streamUrl = "https://example.com/2.m3u8",
                    logoUrl = null
                )
            )
        )
        val viewModel = ChannelGridViewModel(repository)

        var selectedChannel: Channel? = null
        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(
                    viewModel = viewModel,
                    onChannelSelected = { selectedChannel = it }
                )
            }
        }

        // Long press channel with logoUrl -> shows Edit, Redownload Logo, Delete and does NOT trigger playback
        composeTestRule.onNodeWithTag("channel_tile_with-logo").performTouchInput { longClick() }
        assertEquals(null, selectedChannel)
        composeTestRule.onNodeWithTag("context_menu_edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_menu_redownload_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_menu_delete").assertIsDisplayed()

        // Long press channel without logoUrl -> hides Redownload Logo and does NOT trigger playback
        composeTestRule.onNodeWithTag("channel_tile_no-logo").performTouchInput { longClick() }
        assertEquals(null, selectedChannel)
        composeTestRule.onNodeWithTag("context_menu_edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("context_menu_redownload_logo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("context_menu_delete").assertIsDisplayed()
    }

    @Test
    fun editChannelFromContextMenu_updatesDetailsAndHandlesQrScanAndLogoChanges() {
        val sampleBytes = createSamplePngBytes()
        val logoFolder = tempFolder.newFolder("logos_edit")
        val cachedFile = File(logoFolder, "edit-chan.png")
        FileOutputStream(cachedFile).use { it.write(sampleBytes) }

        val storage = FileLogoStorage(
            baseDir = logoFolder,
            imageDownloader = { sampleBytes },
            ioDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        )
        val repository = InMemoryChannelRepository(
            listOf(
                Channel(
                    id = "edit-chan",
                    name = "Initial Channel",
                    streamUrl = "https://example.com/initial.m3u8",
                    logoUrl = "https://example.com/initial.png",
                    localLogoPath = cachedFile.absolutePath
                )
            )
        )
        val viewModel = ChannelGridViewModel(repository, storage)
        val scanner = FakeQrScannerProvider(
            cameraPayload = "https://example.com/scanned.m3u8"
        )

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel, qrScanner = scanner)
            }
        }

        composeTestRule.onNodeWithTag("channel_tile_edit-chan").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("context_menu_edit").performClick()

        // Edit dialog opens with pre-populated values and "Edit Channel" / "Save"
        composeTestRule.onNodeWithText("Edit Channel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_name_input").assertTextContains("Initial Channel")
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://example.com/initial.m3u8")
        composeTestRule.onNodeWithTag("logo_url_input").assertTextContains("https://example.com/initial.png")

        // Use inline QR scan on stream URL field
        composeTestRule.onNodeWithTag("scan_stream_url_qr_button").performClick()
        composeTestRule.onNodeWithTag("fake_camera").performClick()
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://example.com/scanned.m3u8")

        // Also update name and clear logo URL
        composeTestRule.onNodeWithTag("channel_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("Scanned Edit")
        composeTestRule.onNodeWithTag("logo_url_input").performTextClearance()
        composeTestRule.onNodeWithTag("save_channel_button").performClick()
        composeTestRule.waitForIdle()

        // Channel name updated, cached logo deleted, placeholder shown
        composeTestRule.onNodeWithText("Scanned Edit").assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_logo_placeholder", useUnmergedTree = true).assertIsDisplayed()
        assertFalse(cachedFile.exists())
    }

    @Test
    fun redownloadLogoFromContextMenu_forcesFreshDownloadAndRefreshesUi() {
        val sampleBytes = createSamplePngBytes()
        var downloadCalls = 0
        val logoFolder = tempFolder.newFolder("logos_redownload")
        val storage = FileLogoStorage(
            baseDir = logoFolder,
            imageDownloader = {
                downloadCalls++
                sampleBytes
            },
            ioDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        )
        val repository = InMemoryChannelRepository(
            listOf(
                Channel(
                    id = "redownload-chan",
                    name = "Logo Test",
                    streamUrl = "https://example.com/live.m3u8",
                    logoUrl = "https://example.com/logo.png",
                    localLogoPath = null
                )
            )
        )
        val viewModel = ChannelGridViewModel(repository, storage)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        // Initially displays placeholder because localLogoPath is null
        composeTestRule.onNodeWithTag("channel_logo_placeholder", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("channel_tile_redownload-chan").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("context_menu_redownload_logo").performClick()
        composeTestRule.waitForIdle()

        // After redownload, logo image is displayed and download was invoked
        assertEquals(1, downloadCalls)
        composeTestRule.onNodeWithTag("channel_logo_image", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun deleteChannelFromContextMenu_showsConfirmationAndRemovesChannelAndCachedLogo() {
        val sampleBytes = createSamplePngBytes()
        val logoFolder = tempFolder.newFolder("logos_delete")
        val cachedFile = File(logoFolder, "del-chan.png")
        FileOutputStream(cachedFile).use { it.write(sampleBytes) }

        val storage = FileLogoStorage(baseDir = logoFolder, imageDownloader = { sampleBytes })
        val repository = InMemoryChannelRepository(
            listOf(
                Channel(
                    id = "del-chan",
                    name = "To Delete",
                    streamUrl = "https://example.com/del.m3u8",
                    logoUrl = "https://example.com/del.png",
                    localLogoPath = cachedFile.absolutePath
                )
            )
        )
        val viewModel = ChannelGridViewModel(repository, storage)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("channel_tile_del-chan").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("context_menu_delete").performClick()

        // Confirmation dialog appears
        composeTestRule.onNodeWithTag("delete_channel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete To Delete?").assertIsDisplayed()

        // Cancel keeps channel
        composeTestRule.onNodeWithTag("cancel_delete_channel_button").performClick()
        composeTestRule.onNodeWithTag("delete_channel_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("To Delete").assertIsDisplayed()
        assertTrue(cachedFile.exists())

        // Delete again and confirm
        composeTestRule.onNodeWithTag("channel_tile_del-chan").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("context_menu_delete").performClick()
        composeTestRule.onNodeWithTag("confirm_delete_channel_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("To Delete").assertDoesNotExist()
        composeTestRule.onNodeWithTag("empty_channels_view").assertIsDisplayed()
        assertFalse(cachedFile.exists())
    }

    @Test
    fun longPressAndDrag_dismissesContextMenu_andReordersChannelsPersistently() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "drag_reorder_test.db"
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        val repo = SqliteChannelRepository(context, dbName, ioDispatcher = testDispatcher)
        runBlocking {
            repo.addChannel(Channel(id = "c1", name = "First", streamUrl = "https://example.com/1.m3u8"))
            repo.addChannel(Channel(id = "c2", name = "Second", streamUrl = "https://example.com/2.m3u8"))
        }
        // Order initially is c2 (position 0), c1 (position 1)
        val viewModel = ChannelGridViewModel(repo)

        composeTestRule.setContent {
            GoTvTheme {
                ChannelGridScreen(viewModel = viewModel)
            }
        }

        val c2Bounds = composeTestRule.onNodeWithTag("channel_tile_c2").fetchSemanticsNode().boundsInRoot
        val c1Bounds = composeTestRule.onNodeWithTag("channel_tile_c1").fetchSemanticsNode().boundsInRoot
        val gridBounds = composeTestRule.onNodeWithTag("channels_grid").fetchSemanticsNode().boundsInRoot

        val startInGrid = c2Bounds.center - gridBounds.topLeft
        val endInGrid = c1Bounds.center - gridBounds.topLeft
        val delta = endInGrid - startInGrid

        composeTestRule.onNodeWithTag("channels_grid").performTouchInput {
            down(startInGrid)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100L)
            // Drag in steps to trigger slop and reorder
            val steps = 10
            for (i in 1..steps) {
                moveBy(delta / steps.toFloat())
                advanceEventTime(16L)
            }
            up()
        }

        composeTestRule.waitForIdle()

        // Context menu must be dismissed and order swapped to [c1, c2]
        composeTestRule.onNodeWithTag("context_menu_edit").assertDoesNotExist()
        val reorderedIds = runBlocking { repo.getAllChannels().first().map { it.id } }
        assertEquals(listOf("c1", "c2"), reorderedIds)

        // Verify persistence across a new repository instance
        val restartedRepo = SqliteChannelRepository(context, dbName)
        val persistedIds = runBlocking { restartedRepo.getAllChannels().first().map { it.id } }
        assertEquals(listOf("c1", "c2"), persistedIds)
    }
}

