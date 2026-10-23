package com.gotv.player.ui.util

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.gotv.player.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class UiTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun dynamicString_resolvesDirectly() {
        val text = UiText.DynamicString("Hello World")
        assertEquals("Hello World", text.asString(context))
    }

    @Test
    fun dynamicString_resolvesInCompose() {
        val text = UiText.DynamicString("Dynamic Hello")
        var resolved = ""
        composeTestRule.setContent {
            resolved = text.asString()
        }
        assertEquals("Dynamic Hello", resolved)
    }

    @Test
    fun stringResource_withoutArgs_resolvesString() {
        val text = UiText.StringResource(R.string.app_name)
        assertEquals("GoTV", text.asString(context))
    }

    @Test
    fun stringResource_withoutArgs_resolvesInCompose() {
        val text = UiText.StringResource(R.string.dialog_add_channel_title)
        var resolved = ""
        composeTestRule.setContent {
            resolved = text.asString()
        }
        assertEquals("Add Channel", resolved)
    }

    @Test
    fun stringResource_withArgs_formatsString() {
        val text = UiText.StringResource(R.string.about_version_format, "1.0.0")
        assertEquals("v1.0.0", text.asString(context))
    }

    @Test
    fun stringResource_withArgs_resolvesInCompose() {
        val text = UiText.StringResource(R.string.about_version_format, "2.0.0")
        var resolved = ""
        composeTestRule.setContent {
            resolved = text.asString()
        }
        assertEquals("v2.0.0", resolved)
    }

    @Test
    fun pluralResource_resolvesCorrectPluralString() {
        val single = UiText.PluralResource(R.plurals.channel_import_success_count, 1)
        assertEquals("Successfully imported 1 channel", single.asString(context))

        val plural = UiText.PluralResource(R.plurals.channel_import_success_count, 5)
        assertEquals("Successfully imported 5 channels", plural.asString(context))
    }

    @Test
    fun pluralResource_resolvesInCompose() {
        val single = UiText.PluralResource(R.plurals.channel_import_success_count, 1)
        val plural = UiText.PluralResource(R.plurals.channel_import_success_count, 3)
        var singleResolved = ""
        var pluralResolved = ""
        composeTestRule.setContent {
            singleResolved = single.asString()
            pluralResolved = plural.asString()
        }
        assertEquals("Successfully imported 1 channel", singleResolved)
        assertEquals("Successfully imported 3 channels", pluralResolved)
    }

    @Test
    @Config(qualifiers = "tr")
    fun uiText_resolvesTurkishResources() {
        val actionText = UiText.StringResource(R.string.action_add_channel)
        assertEquals("Kanal Ekle", actionText.asString(context))

        val singlePlural = UiText.PluralResource(R.plurals.channel_import_success_count, 1)
        val multiPlural = UiText.PluralResource(R.plurals.channel_import_success_count, 7)
        assertEquals("1 kanal başarıyla içe aktarıldı", singlePlural.asString(context))
        assertEquals("7 kanal başarıyla içe aktarıldı", multiPlural.asString(context))
    }
}
