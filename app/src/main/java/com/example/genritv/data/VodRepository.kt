package com.example.genritv.data

import com.example.genritv.model.VodMovie

object VodRepository {
    // Mock data for VOD with sample posters
    fun getMovies(): List<VodMovie> {
        return listOf(
            VodMovie("The Batman", "url_1", "https://image.tmdb.org/t/p/w500/74xTEgt7R36FpoOqvqLZxcqJA9n.jpg", "Action", "2022"),
            VodMovie("Oppenheimer", "url_2", "https://image.tmdb.org/t/p/w500/8Gxv2mYnrnBpepSgwq1bZtF9dfl.jpg", "Drama", "2023"),
            VodMovie("Spiderman: Across the Spider-Verse", "url_3", "https://image.tmdb.org/t/p/w500/8Vtpi9pR71oqRlvqndvrR21dnjs.jpg", "Animation", "2023"),
            VodMovie("Dune: Part Two", "url_4", "https://image.tmdb.org/t/p/w500/8bBihcbzqS5kU9hsSXYzvlByRuw.jpg", "Sci-Fi", "2024"),
            VodMovie("John Wick: Chapter 4", "url_5", "https://image.tmdb.org/t/p/w500/vZloFAK7NmvMGKE7VkzbIz3Uuqc.jpg", "Action", "2023")
        )
    }
}
