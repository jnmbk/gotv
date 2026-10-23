package com.gotv.player.ui.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * A sealed interface representing UI text that can either be a plain string,
 * a string resource, or a plural resource. Allows ViewModels and business logic
 * to define user-facing text without retaining Android [Context].
 */
sealed interface UiText {

    fun asString(context: Context): String

    @Composable
    fun asString(): String

    data class DynamicString(val value: String) : UiText {
        override fun asString(context: Context): String = value

        @Composable
        override fun asString(): String = value
    }

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())

        override fun asString(context: Context): String {
            return if (args.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *args.toTypedArray())
            }
        }

        @Composable
        override fun asString(): String {
            val context = LocalContext.current
            return asString(context)
        }
    }

    data class PluralResource(
        @PluralsRes val resId: Int,
        val count: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@PluralsRes resId: Int, count: Int, vararg args: Any) : this(resId, count, args.toList())

        override fun asString(context: Context): String {
            val formatArgs = if (args.isEmpty()) arrayOf(count) else args.toTypedArray()
            return context.resources.getQuantityString(resId, count, *formatArgs)
        }

        @Composable
        override fun asString(): String {
            val context = LocalContext.current
            return asString(context)
        }
    }
}
