package com.gotv.player.data.backup

import com.gotv.player.data.model.Channel
import com.gotv.player.data.model.ChannelValidator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class ChannelImportResult {
    data class Success(val channels: List<Channel>) : ChannelImportResult()
    data class Failure(val reason: String) : ChannelImportResult()
}

/**
 * Handles serialization of IPTV channel lists to versioned JSON and parsing/validating
 * imported JSON files.
 */
object ChannelJsonSerializer {

    const val CURRENT_SCHEMA_VERSION = 1

    /**
     * Serializes the given channel list into versioned JSON format.
     */
    fun exportChannels(channels: List<Channel>): String {
        val root = JSONObject()
        root.put("version", CURRENT_SCHEMA_VERSION)

        val channelsArray = JSONArray()
        for (channel in channels) {
            val channelObj = JSONObject()
            channelObj.put("name", channel.name)

            val streamUrlsArray = JSONArray()
            val urls = channel.streamUrls.ifEmpty {
                if (channel.streamUrl.isNotEmpty()) listOf(channel.streamUrl) else emptyList()
            }
            urls.forEach { streamUrlsArray.put(it) }
            channelObj.put("streamUrls", streamUrlsArray)

            if (!channel.logoUrl.isNullOrEmpty()) {
                channelObj.put("logoUrl", channel.logoUrl)
            }
            channelsArray.put(channelObj)
        }
        root.put("channels", channelsArray)
        return root.toString(2)
    }

    /**
     * Parses a JSON string, tolerating legacy and alternative formats, validating channel
     * records and discarding invalid entries.
     */
    fun importChannels(jsonString: String): ChannelImportResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return ChannelImportResult.Failure("Channel file is empty")
        }

        val channelsArray: JSONArray = try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                if (!root.has("channels")) {
                    return ChannelImportResult.Failure("Invalid file format: missing channels list")
                }
                root.optJSONArray("channels")
                    ?: return ChannelImportResult.Failure("Invalid file format: channels is not a valid list")
            }
        } catch (_: JSONException) {
            return ChannelImportResult.Failure("Failed to parse JSON file")
        } catch (_: Exception) {
            return ChannelImportResult.Failure("Failed to read channel file")
        }

        val validChannels = mutableListOf<Channel>()

        for (i in 0 until channelsArray.length()) {
            val itemObj = channelsArray.optJSONObject(i) ?: continue

            val name = itemObj.optString("name", "").trim()

            val rawStreamUrls = mutableListOf<String>()
            val streamUrlsArray = itemObj.optJSONArray("streamUrls")
            if (streamUrlsArray != null) {
                for (j in 0 until streamUrlsArray.length()) {
                    val url = streamUrlsArray.optString(j, "").trim()
                    if (url.isNotEmpty()) {
                        rawStreamUrls.add(url)
                    }
                }
            }

            // Fallback for legacy single "streamUrl" or alternative "url"
            if (rawStreamUrls.isEmpty()) {
                val streamUrl = itemObj.optString("streamUrl", "").trim()
                if (streamUrl.isNotEmpty()) {
                    rawStreamUrls.add(streamUrl)
                } else {
                    val altUrl = itemObj.optString("url", "").trim()
                    if (altUrl.isNotEmpty()) {
                        rawStreamUrls.add(altUrl)
                    }
                }
            }

            val rawLogoUrl = if (itemObj.has("logoUrl") && !itemObj.isNull("logoUrl")) {
                itemObj.optString("logoUrl", "").trim().ifEmpty { null }
            } else {
                null
            }

            val validation = ChannelValidator.validate(
                name = name,
                streamUrls = rawStreamUrls,
                logoUrl = rawLogoUrl
            )

            if (validation.isValid) {
                val normalizedUrls = rawStreamUrls
                    .map { ChannelValidator.normalizeUrl(it) }
                    .filter { it.isNotEmpty() }
                val normalizedLogo = ChannelValidator.normalizeOptionalUrl(rawLogoUrl)

                validChannels.add(
                    Channel(
                        name = name,
                        streamUrls = normalizedUrls,
                        logoUrl = normalizedLogo,
                        position = validChannels.size
                    )
                )
            }
        }

        if (validChannels.isEmpty()) {
            return ChannelImportResult.Failure("No valid channels found in file")
        }

        return ChannelImportResult.Success(validChannels)
    }
}
