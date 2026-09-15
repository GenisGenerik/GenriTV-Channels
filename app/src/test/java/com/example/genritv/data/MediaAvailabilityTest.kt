package com.example.genritv.data

import com.example.genritv.model.Series
import com.example.genritv.model.SeriesEpisode
import com.example.genritv.model.VodMovie
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAvailabilityTest {
    @Test
    fun mock_movie_url_is_not_treated_as_playable() {
        val movie = VodMovie(
            title = "Demo Movie",
            url = "url_1"
        )

        assertFalse(StreamUrlValidator.isPlayableHttpUrl(movie.url))
    }

    @Test
    fun series_is_playable_only_when_it_has_a_valid_episode_source() {
        val unavailable = Series("Demo Series")
        val available = unavailable.copy(
            episodes = listOf(
                SeriesEpisode(
                    title = "Episode 1",
                    url = "https://example.com/episode-1.m3u8",
                    episode = 1
                )
            )
        )

        assertTrue(available.episodes.any { StreamUrlValidator.isPlayableHttpUrl(it.url) })
        assertFalse(unavailable.episodes.any { StreamUrlValidator.isPlayableHttpUrl(it.url) })
    }
}
