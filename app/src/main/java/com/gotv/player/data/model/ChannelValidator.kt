package com.gotv.player.data.model

import java.net.URI

object ChannelValidator {

    data class ValidationResult(
        val nameError: String? = null,
        val streamUrlError: String? = null,
        val logoUrlError: String? = null
    ) {
        val isValid: Boolean
            get() = nameError == null && streamUrlError == null && logoUrlError == null
    }

    fun validate(name: String, streamUrl: String, logoUrl: String? = null): ValidationResult {
        val nameError = when {
            name.isBlank() -> "Channel name cannot be empty"
            else -> null
        }

        val trimmedUrl = streamUrl.trim()
        val streamUrlError = when {
            trimmedUrl.isBlank() -> "Stream URL cannot be empty"
            !isValidStreamUrl(trimmedUrl) -> "Please enter a valid M3U8 URL (e.g. https://example.com/live.m3u8)"
            else -> null
        }

        val trimmedLogo = logoUrl?.trim()
        val logoUrlError = when {
            trimmedLogo.isNullOrEmpty() -> null
            !isValidHttpUrl(trimmedLogo) -> "Please enter a valid logo URL"
            else -> null
        }

        return ValidationResult(
            nameError = nameError,
            streamUrlError = streamUrlError,
            logoUrlError = logoUrlError
        )
    }

    private fun isValidStreamUrl(url: String): Boolean {
        if (!isValidHttpUrl(url)) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val path = uri.path ?: ""
        val query = uri.query ?: ""
        return path.contains(".m3u8", ignoreCase = true) || query.contains(".m3u8", ignoreCase = true)
    }

    private fun isValidHttpUrl(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return runCatching {
            val uri = URI(url)
            !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }
}

