package com.example.genritv.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlValidatorTest {
    @Test
    fun accepts_https_and_http_streams() {
        assertTrue(StreamUrlValidator.isPlayableHttpUrl("https://example.com/live.m3u8"))
        assertTrue(StreamUrlValidator.isPlayableHttpUrl("http://example.com/live.m3u8"))
    }

    @Test
    fun rejects_local_and_malformed_urls() {
        assertFalse(StreamUrlValidator.isPlayableHttpUrl("file:///sdcard/video.m3u8"))
        assertFalse(StreamUrlValidator.isPlayableHttpUrl("content://local/video"))
        assertFalse(StreamUrlValidator.isPlayableHttpUrl("not-a-url"))
        assertFalse(StreamUrlValidator.isPlayableHttpUrl(""))
    }

    @Test
    fun rejects_overly_long_urls() {
        val url = "https://example.com/" + "a".repeat(2_100)
        assertFalse(StreamUrlValidator.isPlayableHttpUrl(url))
    }
}
