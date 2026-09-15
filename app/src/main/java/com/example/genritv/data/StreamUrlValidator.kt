package com.example.genritv.data

import java.net.URI

/**
 * Allows only network stream URLs that Android Media3 can safely resolve.
 * This prevents malformed or local-file URLs from reaching the player.
 */
object StreamUrlValidator {
    fun isPlayableHttpUrl(url: String): Boolean {
        if (url.isBlank() || url.length > 2_048) return false

        return runCatching {
            val uri = URI(url.trim())
            val scheme = uri.scheme?.lowercase()
            !uri.host.isNullOrBlank() && scheme in setOf("http", "https")
        }.getOrDefault(false)
    }
}
