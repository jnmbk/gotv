package com.gotv.player.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.player.FakePlayerEngine
import com.gotv.player.testing.SlowTest
import com.gotv.player.ui.qr.FakeQrScannerProvider
import com.gotv.player.ui.theme.GoTvTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class GoTvAppQrImportEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = InMemoryChannelRepository()

    private fun launch(scanner: FakeQrScannerProvider?) {
        composeTestRule.setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = repository,
                    playerEngineFactory = { FakePlayerEngine() },
                    qrScanner = scanner
                )
            }
        }
        composeTestRule.onNodeWithTag("add_channel_top_bar_button").performClick()
    }

    @Test
    fun dialogLevelQrButtonsAndImagePicker_areRemoved() {
        launch(FakeQrScannerProvider())
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_qr_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("import_qr_image_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("qr_error_text").assertDoesNotExist()
    }

    @Test
    fun inlineQrButtons_hiddenWithoutScanner() {
        launch(null)
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_stream_url_qr_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("scan_logo_url_qr_button").assertDoesNotExist()
    }

    @Test
    fun cameraScan_streamUrlField_populatesOnlyStreamUrlAndSavesChannel() {
        launch(FakeQrScannerProvider(cameraPayload = "https://e.com/live.m3u8"))

        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("News HD")
        composeTestRule.onNodeWithTag("scan_stream_url_qr_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("fake_camera").assertIsDisplayed().performClick()

        composeTestRule.onNodeWithTag("fake_camera").assertDoesNotExist()
        composeTestRule.onNodeWithTag("channel_name_input").assertTextContains("News HD")
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://e.com/live.m3u8")
        composeTestRule.onNodeWithTag("logo_url_input").assertTextContains("https://")

        composeTestRule.onNodeWithTag("save_channel_button").performClick()
        composeTestRule.waitForIdle()
        val saved = runBlocking { repository.getAllChannels().first() }
        assertEquals(listOf("News HD"), saved.map { it.name })
        assertEquals("https://e.com/live.m3u8", saved.first().streamUrl)
    }

    @Test
    fun cameraScan_logoUrlField_populatesOnlyLogoUrl() {
        launch(FakeQrScannerProvider(cameraPayload = "https://e.com/logo.png"))

        composeTestRule.onNodeWithTag("stream_url_input").performTextClearance()
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://example.com/stream.m3u8")
        composeTestRule.onNodeWithTag("scan_logo_url_qr_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("fake_camera").assertIsDisplayed().performClick()

        composeTestRule.onNodeWithTag("fake_camera").assertDoesNotExist()
        composeTestRule.onNodeWithTag("logo_url_input").assertTextContains("https://e.com/logo.png")
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://example.com/stream.m3u8")
    }

    @Test
    fun cameraScan_dismissScanner_leavesInputsUnchanged() {
        launch(FakeQrScannerProvider(cameraPayload = "https://new.com/stream.m3u8"))

        composeTestRule.onNodeWithTag("stream_url_input").performTextClearance()
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://original.com/live.m3u8")
        composeTestRule.onNodeWithTag("scan_stream_url_qr_button").performClick()
        composeTestRule.onNodeWithTag("fake_camera").assertIsDisplayed()

        composeTestRule.onNodeWithTag("qr_scanner_close_button").performClick()
        composeTestRule.onNodeWithTag("fake_camera").assertDoesNotExist()
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://original.com/live.m3u8")
    }
}
