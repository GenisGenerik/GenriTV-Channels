package com.example.genritv.data

import com.example.genritv.model.Series

object SeriesRepository {
    // Mock data for Series with sample posters
    fun getSeries(): List<Series> {
        return listOf(
            Series("Stranger Things", "https://image.tmdb.org/t/p/w500/49WJfev0moxb9bOaYNoSTpiV1v2.jpg", "Sci-Fi", 4),
            Series("The Last of Us", "https://image.tmdb.org/t/p/w500/uKvH5j21s2B4sry2d49bsFZQjz3.jpg", "Drama", 1),
            Series("The Boys", "https://image.tmdb.org/t/p/w500/7Ns9tKHuDQp2hLSqzjr7O3M3bH.jpg", "Action", 3),
            Series("Breaking Bad", "https://image.tmdb.org/t/p/w500/ggws3q95oTz696y5yUXD8749yEq.jpg", "Crime", 5),
            Series("House of the Dragon", "https://image.tmdb.org/t/p/w500/1X4h40OfSwwUqC2sJfEkzUBpLNZ.jpg", "Fantasy", 2)
        )
    }
}
