package com.example.genritv.data

import com.example.genritv.model.TvChannel

/** Centralized TV grouping rules. Keeps classification logic out of UI/ViewModel code. */
object ChannelClassifier {
    private val nationalKeywords = setOf(
        "nasional", "indonesia", "rcti", "sctv", "indosiar", "antv",
        "trans", "tvone", "metro", "kompas", "mnc", "gtv", "inews",
        "tvri", "rtv", "net", "garuda", "moji", "daai"
    )

    fun isNational(channel: TvChannel): Boolean {
        val haystack = listOf(channel.grup.orEmpty(), channel.nama, channel.tvgId.orEmpty())
        return nationalKeywords.any { keyword ->
            haystack.any { value -> value.contains(keyword, ignoreCase = true) }
        }
    }

    fun isRegional(channel: TvChannel): Boolean {
        val group = channel.grup.orEmpty()
        return group.contains("regional", ignoreCase = true) ||
            group.contains("daerah", ignoreCase = true)
    }
}
