package com.gotv.player.data.qr

import com.gotv.player.data.model.ChannelValidator
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/** Channel details extracted from a QR code payload. */
sealed interface QrParseResult {
    data class Success(
        val name: String,
        val streamUrl: String,
        val logoUrl: String? = null
    ) : QrParseResult

    data class Invalid(val reason: String) : QrParseResult
}

/**
 * Parses QR payloads into channel details. Supported formats:
 * - a plain M3U8 URL (name is derived from the URL)
 * - JSON: `{"name": "...", "url": "...", "logo": "..."}`
 * - deep link: `gotv://channel?name=...&url=...&logo=...`
 */
object QrChannelParser {

    private val streamKeys = listOf("url", "streamUrl", "stream_url", "stream")
    private val nameKeys = listOf("name", "title")
    private val logoKeys = listOf("logo", "logoUrl", "logo_url")

    fun parse(raw: String?): QrParseResult {
        val payload = raw?.trim().orEmpty()
        if (payload.isEmpty()) return QrParseResult.Invalid("The QR code is empty")

        val fields = when {
            payload.startsWith("{") -> parseJson(payload)
            payload.startsWith("gotv://", ignoreCase = true) -> parseDeepLink(payload)
            else -> RawFields(name = null, streamUrl = payload, logoUrl = null)
        } ?: return QrParseResult.Invalid("The QR code does not contain valid channel data")

        val streamUrl = ChannelValidator.normalizeUrl(fields.streamUrl.orEmpty())
        val logoUrl = ChannelValidator.normalizeOptionalUrl(fields.logoUrl)
        val name = fields.name?.trim().orEmpty().ifEmpty { deriveName(streamUrl) }

        val validation = ChannelValidator.validate(name.ifEmpty { "Channel" }, streamUrl, logoUrl)
        return when {
            validation.streamUrlError != null ->
                QrParseResult.Invalid("The QR code does not contain a valid stream link")
            validation.logoUrlError != null ->
                QrParseResult.Invalid("The QR code contains an invalid logo URL")
            else -> QrParseResult.Success(name = name, streamUrl = streamUrl, logoUrl = logoUrl)
        }
    }

    private data class RawFields(val name: String?, val streamUrl: String?, val logoUrl: String?)

    private fun parseJson(payload: String): RawFields? {
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        fun first(keys: List<String>) = keys.firstNotNullOfOrNull { key ->
            json.optString(key, "").takeIf { it.isNotBlank() }
        }
        return RawFields(first(nameKeys), first(streamKeys), first(logoKeys))
    }

    private fun parseDeepLink(payload: String): RawFields? {
        val query = payload.substringAfter('?', missingDelimiterValue = "")
            .substringBefore('#')
        if (query.isEmpty()) return null
        val params = runCatching {
            query.split('&').filter { it.isNotEmpty() }.associate { pair ->
                val key = URLDecoder.decode(pair.substringBefore('='), "UTF-8")
                val value = URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
                key to value
            }
        }.getOrNull() ?: return null
        fun first(keys: List<String>) = keys.firstNotNullOfOrNull { params[it] }
        return RawFields(first(nameKeys), first(streamKeys), first(logoKeys))
    }

    private fun deriveName(streamUrl: String): String {
        val uri = runCatching { URI(streamUrl) }.getOrNull() ?: return ""
        val stem = uri.path.orEmpty().trimEnd('/').substringAfterLast('/').substringBeforeLast('.')
        return stem.ifBlank { uri.host.orEmpty() }
    }
}
