package com.example.genritv.data

import com.example.genritv.model.TvChannel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelClassifierTest {
    @Test
    fun national_channel_is_detected_from_name() {
        val channel = TvChannel(
            nama = "RCTI",
            urls = listOf("https://example.com/live.m3u8")
        )

        assertTrue(ChannelClassifier.isNational(channel))
        assertFalse(ChannelClassifier.isRegional(channel))
    }

    @Test
    fun regional_channel_is_detected_from_group() {
        val channel = TvChannel(
            nama = "TV Daerah",
            urls = listOf("https://example.com/live.m3u8"),
            grup = "Regional Jawa Barat"
        )

        assertTrue(ChannelClassifier.isRegional(channel))
    }

    @Test
    fun blank_group_does_not_crash_classifier() {
        val channel = TvChannel(
            nama = "Example TV",
            urls = emptyList(),
            grup = null,
            tvgId = null
        )

        assertFalse(ChannelClassifier.isNational(channel))
        assertFalse(ChannelClassifier.isRegional(channel))
    }
}
