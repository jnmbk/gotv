package com.gotv.player.ui.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gotv.player.BuildConfig
import com.gotv.player.testing.SlowTest
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
class AboutDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutDialog_displaysTitleVersionAuthorLicenseAndDismissButton() {
        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {},
                    onOpenUrl = {}
                )
            }
        }

        // Title
        composeTestRule.onNodeWithTag("about_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_dialog_title").assertIsDisplayed().assertTextContains("GoTV")

        // Version (uses BuildConfig.VERSION_NAME by default)
        composeTestRule.onNodeWithTag("about_dialog_version")
            .assertIsDisplayed()
            .assertTextContains("v${BuildConfig.VERSION_NAME}")

        // Author section
        composeTestRule.onNodeWithText("Author").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_dialog_author_link")
            .assertIsDisplayed()
            .assertTextContains("Uğur Çetin")

        // License section
        composeTestRule.onNodeWithText("License").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_dialog_license_notice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_dialog_license_link")
            .assertIsDisplayed()
            .assertTextContains("Apache License 2.0")

        // Dismiss button
        composeTestRule.onNodeWithTag("about_dialog_dismiss_button").assertIsDisplayed()
    }

    @Test
    fun aboutDialog_tappingAuthorLink_opensAuthorUrl() {
        var openedUrl: String? = null

        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {},
                    authorUrl = "https://github.com/jnmbk",
                    onOpenUrl = { openedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("about_dialog_author_link").performClick()
        assertEquals("https://github.com/jnmbk", openedUrl)
    }

    @Test
    fun aboutDialog_tappingLicenseLink_opensLicenseUrl() {
        var openedUrl: String? = null

        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {},
                    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
                    onOpenUrl = { openedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("about_dialog_license_link").performClick()
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0", openedUrl)
    }

    @Test
    fun aboutDialog_tappingDismissButton_invokesDismissCallback() {
        var dismissed = false

        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("about_dialog_dismiss_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun aboutDialog_customVersionName_isFormattedCorrectly() {
        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {},
                    versionName = "2.1.0-alpha01"
                )
            }
        }

        composeTestRule.onNodeWithTag("about_dialog_version")
            .assertIsDisplayed()
            .assertTextContains("v2.1.0-alpha01")
    }

    @Test
    fun aboutDialog_defaultUriHandler_handlesClicksGracefully() {
        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {}
                )
            }
        }

        // Tapping links with default URI handler shouldn't throw an unhandled crash
        composeTestRule.onNodeWithTag("about_dialog_author_link").performClick()
        composeTestRule.onNodeWithTag("about_dialog_license_link").performClick()
    }
}
