package com.gotv.player.ui.channel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.gotv.player.testing.SlowTest
import com.gotv.player.ui.qr.FakeQrScannerProvider
import com.gotv.player.ui.theme.GoTvTheme
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
class ImportFromUrlDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_rendersTitleInputAndButtons_whenVisible() {
        var dismissed = false
        var submitted = false

        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://"
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = { submitted = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("import_from_url_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import from URL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_url_input").assertIsDisplayed().assertTextContains("https://")
        composeTestRule.onNodeWithTag("submit_import_url_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("cancel_import_url_button").assertIsDisplayed().assertIsEnabled()

        composeTestRule.onNodeWithTag("cancel_import_url_button").performClick()
        assertTrue(dismissed)

        composeTestRule.onNodeWithTag("submit_import_url_button").performClick()
        assertTrue(submitted)
    }

    @Test
    fun clearButton_clearsUrl_whenInputIsNotEmpty() {
        var cleared = false

        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://example.com/channels.json"
                    ),
                    onUrlChange = {},
                    onClearUrl = { cleared = true },
                    onSubmit = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("clear_import_url_button").assertIsDisplayed().performClick()
        assertTrue(cleared)
    }

    @Test
    fun clearButton_isNotShown_whenInputIsEmpty() {
        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = ""
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("clear_import_url_button").assertDoesNotExist()
    }

    @Test
    fun qrScannerButton_isShownWhenProviderProvided_andInvokesCallback() {
        var scanQrCalled = false
        val fakeScanner = FakeQrScannerProvider()

        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://"
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = {},
                    onDismiss = {},
                    qrScanner = fakeScanner,
                    onScanQr = { scanQrCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("scan_import_url_qr_button").assertIsDisplayed().performClick()
        assertTrue(scanQrCalled)
    }

    @Test
    fun qrScannerButton_isNotShownWhenProviderIsNull() {
        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://"
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = {},
                    onDismiss = {},
                    qrScanner = null
                )
            }
        }

        composeTestRule.onNodeWithTag("scan_import_url_qr_button").assertDoesNotExist()
    }

    @Test
    fun inlineError_isDisplayed_whenImportUrlErrorIsSet() {
        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://bad",
                        importUrlError = "Failed to download channel list: HTTP 404"
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("import_url_error_text", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("Failed to download channel list: HTTP 404")
    }

    @Test
    fun loadingState_disablesButtonsAndShowsSpinner() {
        var dismissClicked = false
        var submitClicked = false

        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://example.com/playlist.json",
                        isImportUrlLoading = true
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = { submitClicked = true },
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("import_url_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_import_url_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("cancel_import_url_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("clear_import_url_button").assertIsNotEnabled()

        assertFalse(submitClicked)
        assertFalse(dismissClicked)
    }
}
