package com.gotv.player.ui.qr

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gotv.player.MainActivity
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AndroidQrScannerProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hasCamera_returnsFalse_whenNoCamerasExist() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertFalse(AndroidQrScannerProvider.hasCamera(app))
    }

    @Test
    fun hasCamera_returnsFalse_whenCameraServiceIsNull() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        @Suppress("DEPRECATION")
        shadowOf(app).setSystemService(Context.CAMERA_SERVICE, null)
        assertFalse(AndroidQrScannerProvider.hasCamera(app))
    }

    @Test
    fun hasCamera_returnsTrue_whenCameraIsAvailable() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).set(
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.LENS_FACING_BACK
        )
        shadowOf(cameraManager).addCamera("0", characteristics)

        assertTrue(AndroidQrScannerProvider.hasCamera(app))
    }

    @Test
    fun cameraScanner_whenDeviceHasNoCamera_showsUnavailableMessageAndCanClose() {
        var closed = false
        composeTestRule.setContent {
            GoTvTheme {
                AndroidQrScannerProvider.CameraScanner(
                    onQrDetected = {},
                    onClose = { closed = true },
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithTag("qr_camera_unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera is not available on this device.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("qr_scanner_close_button").performClick()
        assertTrue(closed)
    }

    @Test
    fun cameraPreview_whenCameraProviderInitFails_reportsUnavailableWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        @Suppress("DEPRECATION")
        shadowOf(app).setSystemService(Context.CAMERA_SERVICE, null)

        var unavailableReported = false
        composeTestRule.setContent {
            GoTvTheme {
                AndroidQrScannerProvider.CameraPreview(
                    onQrDetected = {},
                    onCameraUnavailable = { unavailableReported = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        val future = ProcessCameraProvider.getInstance(app)
        repeat(10) {
            if (!future.isDone) {
                ShadowSystemClock.advanceBy(Duration.ofMillis(500))
                Thread.sleep(50)
            }
        }
        runCatching { future.get(2, TimeUnit.SECONDS) }

        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        assertTrue("Expected CameraPreview to report camera unavailable on init failure", unavailableReported)
    }
}

@RunWith(RobolectricTestRunner::class)
class MainActivityNoCameraTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addingChannel_onDeviceWithNoCamera_hidesQrButtonsAndSavesChannel() {
        composeTestRule.onNodeWithTag("add_channel_top_bar_button").performClick()
        composeTestRule.onNodeWithTag("add_channel_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("scan_stream_url_qr_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("scan_logo_url_qr_button").assertDoesNotExist()

        composeTestRule.onNodeWithTag("channel_name_input").performTextInput("TV Channel")
        composeTestRule.onNodeWithTag("stream_url_input").performTextClearance()
        composeTestRule.onNodeWithTag("stream_url_input").performTextInput("https://example.com/live.m3u8")
        composeTestRule.onNodeWithTag("save_channel_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("TV Channel").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("TV Channel").assertIsDisplayed()
    }

    @Test
    fun importFromUrl_onDeviceWithNoCamera_hidesQrButton() {
        composeTestRule.onNodeWithTag("channel_overflow_menu_button").performClick()
        composeTestRule.onNodeWithTag("menu_import_from_url").performClick()
        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("scan_import_url_qr_button").assertDoesNotExist()
    }
}
