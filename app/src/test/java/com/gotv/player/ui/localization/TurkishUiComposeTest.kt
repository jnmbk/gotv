package com.gotv.player.ui.localization

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gotv.player.R
import com.gotv.player.ui.about.AboutDialog
import com.gotv.player.ui.channel.AddChannelDialog
import com.gotv.player.ui.channel.ChannelGridUiState
import com.gotv.player.ui.channel.EmptyChannelsView
import com.gotv.player.ui.channel.ImportFromUrlDialog
import com.gotv.player.ui.theme.GoTvTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "tr")
class TurkishUiComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyChannelsView_rendersTurkishLabels() {
        var addClicked = false
        composeTestRule.setContent {
            GoTvTheme {
                EmptyChannelsView(onAddChannelClick = { addClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Henüz kanal yok").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canlı yayınları izlemeye başlamak için ilk IPTV kanalınızı ekleyin.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kanal Ekle").assertIsDisplayed().performClick()
        assertTrue(addClicked)
    }

    @Test
    fun addChannelDialog_rendersTurkishLabelsAndButtons() {
        var dismissed = false
        var confirmed = false
        composeTestRule.setContent {
            GoTvTheme {
                AddChannelDialog(
                    uiState = ChannelGridUiState(
                        isAddDialogOpen = true,
                        nameInput = "",
                        streamUrlsInput = listOf(""),
                        logoUrlInput = ""
                    ),
                    onNameChange = {},
                    onStreamUrlChange = { _, _ -> },
                    onAddStreamUrl = {},
                    onRemoveStreamUrl = {},
                    onLogoUrlChange = {},
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Kanal Ekle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kanal Adı").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yayın URL'si").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logo URL'si (İsteğe bağlı)").assertIsDisplayed()
        composeTestRule.onNodeWithText("İptal").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Ekle").assertIsDisplayed().performClick()
        assertTrue(dismissed)
        assertTrue(confirmed)
    }

    @Test
    fun importFromUrlDialog_rendersTurkishLabels() {
        var submitClicked = false
        var dismissClicked = false
        composeTestRule.setContent {
            GoTvTheme {
                ImportFromUrlDialog(
                    uiState = ChannelGridUiState(
                        isImportUrlDialogOpen = true,
                        importUrlInput = "https://example.com/playlist.json"
                    ),
                    onUrlChange = {},
                    onClearUrl = {},
                    onSubmit = { submitClicked = true },
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("URL'den İçe Aktar").assertIsDisplayed()
        composeTestRule.onNodeWithText("URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("İçe Aktar").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("İptal").assertIsDisplayed().performClick()
        assertTrue(submitClicked)
        assertTrue(dismissClicked)
    }

    @Test
    fun deleteChannelStrings_resolveWithParametersInTurkish() {
        composeTestRule.setContent {
            GoTvTheme {
                Text(text = stringResource(R.string.dialog_delete_channel_title, "TRT 1"))
                Text(text = stringResource(R.string.dialog_delete_channel_message, "TRT 1"))
            }
        }

        composeTestRule.onNodeWithText("TRT 1 silinsin mi?").assertIsDisplayed()
        composeTestRule.onNodeWithText("TRT 1 kanalını silmek istediğinizden emin misiniz?").assertIsDisplayed()
    }

    @Test
    fun importConfirmStrings_resolveInTurkish() {
        composeTestRule.setContent {
            GoTvTheme {
                Text(text = stringResource(R.string.dialog_import_confirm_title))
                Text(text = stringResource(R.string.dialog_import_confirm_message))
                Text(text = stringResource(R.string.action_proceed))
            }
        }

        composeTestRule.onNodeWithText("Tüm kanallar değiştirilsin mi?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bu işlem mevcut tüm kanalların yerine yenilerini koyacaktır. Devam edilsin mi?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Devam Et").assertIsDisplayed()
    }

    @Test
    fun playerErrorStrings_resolveInTurkish() {
        composeTestRule.setContent {
            GoTvTheme {
                Text(text = stringResource(R.string.player_error_message))
                Text(text = stringResource(R.string.player_error_retry))
                Text(text = stringResource(R.string.player_error_back))
            }
        }

        composeTestRule.onNodeWithText("Bu kanal oynatılamıyor. Bağlantınızı kontrol edip tekrar deneyin.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tekrar dene").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kanallara dön").assertIsDisplayed()
    }

    @Test
    fun aboutDialog_rendersTurkishSectionLabels() {
        var openedUrl: String? = null
        composeTestRule.setContent {
            GoTvTheme {
                AboutDialog(
                    onDismissRequest = {},
                    onOpenUrl = { openedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Kaynak Kodu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yazar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lisans").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kapat").assertIsDisplayed()
        composeTestRule.onNodeWithText("GoTV, Apache License 2.0 altında lisanslanmış açık kaynaklı bir yazılımdır.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_dialog_source_code_link").performClick()
        assertEquals("https://github.com/jnmbk/gotv", openedUrl)
    }
}
