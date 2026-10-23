package com.gotv.player.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.gotv.player.data.backup.ChannelListDownloader
import com.gotv.player.data.backup.DownloadResult
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.testing.SlowTest
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class GoTvAppUrlImportEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val initialChannel = Channel(
        id = "initial-channel-1",
        name = "Initial Channel",
        streamUrl = "https://example.com/initial.m3u8"
    )

    private val sampleJson = """
        {
          "version": 1,
          "channels": [
            {
              "name": "E2E Channel 1",
              "streamUrls": ["https://live.example.com/c1.m3u8"]
            },
            {
              "name": "E2E Channel 2",
              "streamUrls": ["https://live.example.com/c2.m3u8"]
            }
          ]
        }
    """.trimIndent()

    private fun launchApp(
        repository: InMemoryChannelRepository,
        downloader: ChannelListDownloader,
        qrScanner: FakeQrScannerProvider? = null
    ) {
        composeTestRule.setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = repository,
                    playerEngineFactory = { FakePlayerEngine() },
                    qrScanner = qrScanner,
                    channelListDownloader = downloader
                )
            }
        }
    }

    @Test
    fun urlImport_successPath_downloadsChannels_confirmsReplacement_updatesGridAndShowsSnackbar() {
        val repository = InMemoryChannelRepository(listOf(initialChannel))
        var requestedUrl: String? = null
        val downloader = ChannelListDownloader { url ->
            requestedUrl = url
            DownloadResult.Success(sampleJson)
        }

        launchApp(repository, downloader)

        // 1. Initial state check
        composeTestRule.onNodeWithTag("channel_grid_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Initial Channel").assertIsDisplayed()

        // 2. Open overflow menu and tap "Import from URL"
        composeTestRule.onNodeWithTag("channel_overflow_menu_button").performClick()
        composeTestRule.onNodeWithTag("menu_import_from_url").assertIsDisplayed().performClick()

        // 3. Import dialog is displayed
        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_import_url_button").performClick()
        composeTestRule.onNodeWithTag("import_url_input").performTextInput("https://remote.test/playlist.json")

        // 4. Submit import URL
        composeTestRule.onNodeWithTag("submit_import_url_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("https://remote.test/playlist.json", requestedUrl)

        // 5. Replacement confirmation dialog appears
        composeTestRule.onNodeWithTag("import_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Replace all channels?").assertIsDisplayed()

        // 6. Confirm replacement
        composeTestRule.onNodeWithTag("confirm_import_button").performClick()
        composeTestRule.waitForIdle()

        // 7. Verify dialog is dismissed, channel list is updated in repo and UI
        composeTestRule.onNodeWithTag("import_confirm_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("import_from_url_dialog").assertDoesNotExist()

        val updatedChannels = runBlocking { repository.getAllChannels().first() }
        assertEquals(2, updatedChannels.size)
        assertEquals(listOf("E2E Channel 1", "E2E Channel 2"), updatedChannels.map { it.name })

        composeTestRule.onNodeWithText("E2E Channel 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("E2E Channel 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Initial Channel").assertDoesNotExist()

        // 8. Verify success snackbar feedback is displayed
        composeTestRule.onNodeWithText("Successfully imported 2 channels").assertIsDisplayed()
    }

    @Test
    fun urlImport_networkFailurePath_displaysInlineError_andDialogRemainsOpenUntilCancelled() {
        val repository = InMemoryChannelRepository(listOf(initialChannel))
        val downloader = ChannelListDownloader {
            DownloadResult.Failure("Network error: failed to connect to server")
        }

        launchApp(repository, downloader)

        // Open overflow menu -> Import from URL
        composeTestRule.onNodeWithTag("channel_overflow_menu_button").performClick()
        composeTestRule.onNodeWithTag("menu_import_from_url").performClick()

        // Enter URL and submit
        composeTestRule.onNodeWithTag("clear_import_url_button").performClick()
        composeTestRule.onNodeWithTag("import_url_input").performTextInput("https://broken.test/channels.json")
        composeTestRule.onNodeWithTag("submit_import_url_button").performClick()
        composeTestRule.waitForIdle()

        // Confirmation dialog must NOT be opened
        composeTestRule.onNodeWithTag("import_confirm_dialog").assertDoesNotExist()

        // Inline error shown in dialog, and dialog remains open
        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network error: failed to connect to server").assertIsDisplayed()

        // Channels should remain unchanged
        val channels = runBlocking { repository.getAllChannels().first() }
        assertEquals(1, channels.size)
        assertEquals("Initial Channel", channels.first().name)

        // Cancel closes the dialog
        composeTestRule.onNodeWithTag("cancel_import_url_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("import_from_url_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("Initial Channel").assertIsDisplayed()
    }

    @Test
    fun urlImport_qrCodeScanning_populatesInputUrlField() {
        val repository = InMemoryChannelRepository(listOf(initialChannel))
        val qrScanner = FakeQrScannerProvider(cameraPayload = "https://scanned.domain.com/list.json")
        val downloader = ChannelListDownloader { DownloadResult.Success(sampleJson) }

        launchApp(repository, downloader, qrScanner)

        // Open overflow menu -> Import from URL
        composeTestRule.onNodeWithTag("channel_overflow_menu_button").performClick()
        composeTestRule.onNodeWithTag("menu_import_from_url").performClick()

        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()

        // Click QR scan button in URL dialog
        composeTestRule.onNodeWithTag("scan_import_url_qr_button").assertIsDisplayed().performClick()

        // Scanner camera overlay is opened
        composeTestRule.onNodeWithTag("fake_camera").assertIsDisplayed().performClick()

        // Scanner closes and input field is populated with scanned URL
        composeTestRule.onNodeWithTag("fake_camera").assertDoesNotExist()
        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_url_input").assertTextContains("https://scanned.domain.com/list.json")

        // Submitting proceeds with scanned URL
        composeTestRule.onNodeWithTag("submit_import_url_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("import_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_import_button").performClick()
        composeTestRule.waitForIdle()

        val updatedChannels = runBlocking { repository.getAllChannels().first() }
        assertEquals(2, updatedChannels.size)
        assertEquals(listOf("E2E Channel 1", "E2E Channel 2"), updatedChannels.map { it.name })
    }
}
