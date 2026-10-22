package com.gotv.player.data.model

import java.net.URI

object ChannelValidator {

    const val DEFAULT_URL_PREFIX = "https://"

    data class ValidationResult(
        val nameError: String? = null,
        val streamUrlError: String? = null,
        val streamUrlsErrors: List<String?> = emptyList(),
        val logoUrlError: String? = null
    ) {
        val isValid: Boolean
            get() = nameError == null && streamUrlError == null && streamUrlsErrors.all { it == null } && logoUrlError == null
    }

    /**
     * Collapse duplicate scheme prefixes when a full URL is typed or pasted into a field
     * already pre-filled with `https://` or `http://`.
     */
    fun stripDuplicateScheme(url: String): String {
        val trimmed = url.trimStart()
        return when {
            trimmed.startsWith("https://https://", ignoreCase = true) ->
                trimmed.substring("https://".length)
            trimmed.startsWith("https://http://", ignoreCase = true) ->
                trimmed.substring("https://".length)
            trimmed.startsWith("http://https://", ignoreCase = true) ->
                trimmed.substring("http://".length)
            trimmed.startsWith("http://http://", ignoreCase = true) ->
                trimmed.substring("http://".length)
            else -> url
        }
    }

    /**
     * Normalizes a handwritten or imported URL by trimming whitespace, treating a bare
     * scheme prefix (`https://` or `http://`) as empty, and prepending `https://` when
     * no scheme is present.
     */
    fun normalizeUrl(url: String): String {
        val cleaned = stripDuplicateScheme(url.trim()).trim()
        if (cleaned.isEmpty() ||
            cleaned.equals("https://", ignoreCase = true) ||
            cleaned.equals("http://", ignoreCase = true)
        ) {
            return ""
        }
        return if (!cleaned.contains("://")) {
            "$DEFAULT_URL_PREFIX$cleaned"
        } else {
            cleaned
        }
    }

    fun normalizeOptionalUrl(url: String?): String? =
        normalizeUrl(url.orEmpty()).ifEmpty { null }

    fun validate(name: String, streamUrl: String, logoUrl: String? = null): ValidationResult =
        validate(name = name, streamUrls = listOf(streamUrl), logoUrl = logoUrl)

    fun validate(name: String, streamUrls: List<String>, logoUrl: String? = null): ValidationResult {
        val nameError = when {
            name.isBlank() -> "Channel name cannot be empty"
            else -> null
        }

        val streamUrlsErrors = if (streamUrls.isEmpty()) {
            listOf("Stream URL cannot be empty")
        } else {
            streamUrls.map { url ->
                val normalizedStreamUrl = normalizeUrl(url)
                when {
                    normalizedStreamUrl.isEmpty() -> "Stream URL cannot be empty"
                    !isValidHttpUrl(normalizedStreamUrl) -> "Please enter a valid stream URL"
                    else -> null
                }
            }
        }
        val streamUrlError = streamUrlsErrors.firstOrNull { it != null }

        val normalizedLogo = normalizeOptionalUrl(logoUrl)
        val logoUrlError = when {
            normalizedLogo == null -> null
            !isValidHttpUrl(normalizedLogo) -> "Please enter a valid logo URL"
            else -> null
        }

        return ValidationResult(
            nameError = nameError,
            streamUrlError = streamUrlError,
            streamUrlsErrors = streamUrlsErrors,
            logoUrlError = logoUrlError
        )
    }

    private fun isValidHttpUrl(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return runCatching {
            val uri = URI(url)
            val host = uri.host ?: return false
            isValidHost(host)
        }.getOrDefault(false)
    }

    private fun isValidHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        if (host.startsWith("[") && host.endsWith("]")) return host.length > 2
        val parts = host.split('.')
        return parts.size >= 2 && parts.all { it.isNotBlank() }
    }
}

