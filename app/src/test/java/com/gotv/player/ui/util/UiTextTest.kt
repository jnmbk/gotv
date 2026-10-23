package com.gotv.player.ui.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gotv.player.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UiTextTest {

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
    fun stringResource_withoutArgs_resolvesString() {
        val text = UiText.StringResource(R.string.app_name)
        assertEquals("GoTV", text.asString(context))
    }

    @Test
    fun stringResource_withArgs_formatsString() {
        val text = UiText.StringResource(R.string.about_version_format, "1.0.0")
        assertEquals("v1.0.0", text.asString(context))
    }

    @Test
    fun pluralResource_resolvesCorrectPluralString() {
        val single = UiText.PluralResource(R.plurals.channel_import_success_count, 1)
        assertEquals("Successfully imported 1 channel", single.asString(context))

        val plural = UiText.PluralResource(R.plurals.channel_import_success_count, 5)
        assertEquals("Successfully imported 5 channels", plural.asString(context))
    }
}
