package com.example.genritv.data

import com.example.genritv.model.TvChannel

object M3uParser {
    
    private val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
    private val groupRegex = """group-title="([^"]+)"""".toRegex()
    private val idRegex = """tvg-id="([^"]+)"""".toRegex()

    fun parse(content: String): List<TvChannel> {
        val channels = mutableListOf<TvChannel>()
        
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null

        content.lineSequence().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                // Extract attributes using pre-compiled regex for speed
                currentLogo = logoRegex.find(trimmedLine)?.groupValues?.get(1)
                currentGroup = groupRegex.find(trimmedLine)?.groupValues?.get(1)
                currentTvgId = idRegex.find(trimmedLine)?.groupValues?.get(1)

                // Extract name (usually after the last comma)
                currentName = trimmedLine.substringAfterLast(",").trim()
            } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                if (currentName.isNotEmpty()) {
                    channels.add(TvChannel(currentName, listOf(trimmedLine), currentLogo, currentGroup, currentTvgId))
                    // Fast reset
                    currentName = ""
                    currentLogo = null
                    currentGroup = null
                    currentTvgId = null
                }
            }
        }
        return channels
    }
}