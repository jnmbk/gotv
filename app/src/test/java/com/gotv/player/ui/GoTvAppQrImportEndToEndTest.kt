package com.gotv.player.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    fun scanOptions_hiddenWithoutScanner() {
        launch(null)
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scan_qr_button").assertDoesNotExist()
    }

    @Test
    fun cameraScan_prefillsFormAndSavesChannel() {
        val json = """{"name":"News HD","url":"https://e.com/live.m3u8","logo":"https://e.com/l.png"}"""
        launch(FakeQrScannerProvider(cameraPayload = json))

        composeTestRule.onNodeWithTag("scan_qr_button").performClick()
        composeTestRule.onNodeWithTag("fake_camera").performClick()

        composeTestRule.onNodeWithTag("fake_camera").assertDoesNotExist()
        composeTestRule.onNodeWithTag("channel_name_input").assertTextContains("News HD")
        composeTestRule.onNodeWithTag("stream_url_input").assertTextContains("https://e.com/live.m3u8")
        composeTestRule.onNodeWithTag("logo_url_input").assertTextContains("https://e.com/l.png")

        composeTestRule.onNodeWithTag("save_channel_button").performClick()
        composeTestRule.waitForIdle()
        val saved = runBlocking { repository.getAllChannels().first() }
        assertEquals(listOf("News HD"), saved.map { it.name })
    }

    @Test
    fun imageImport_prefillsForm() {
        launch(FakeQrScannerProvider(imagePayload = "https://e.com/streams/sports.m3u8"))

        composeTestRule.onNodeWithTag("import_qr_image_button").performClick()

        composeTestRule.onNodeWithTag("channel_name_input").assertTextContains("sports")
        composeTestRule.onNodeWithTag("stream_url_input")
            .assertTextContains("https://e.com/streams/sports.m3u8")
        composeTestRule.onNodeWithTag("qr_error_text").assertDoesNotExist()
    }

    @Test
    fun invalidCameraQr_showsError() {
        launch(FakeQrScannerProvider(cameraPayload = "just some text"))

        composeTestRule.onNodeWithTag("scan_qr_button").performClick()
        composeTestRule.onNodeWithTag("fake_camera").performClick()

        composeTestRule.onNodeWithTag("qr_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stream_url_input").assertTextEquals("Stream URL (M3U8)", "")
    }

    @Test
    fun imageWithoutQr_showsError() {
        launch(FakeQrScannerProvider(imagePayload = null))

        composeTestRule.onNodeWithTag("import_qr_image_button").performClick()

        composeTestRule.onNodeWithTag("qr_error_text").assertIsDisplayed()
    }
}
