package com.example.genritv.model

data class Series(
    val title: String,
    val logo: String? = null,
    val genre: String? = null,
    val seasons: Int = 1,
    val episodes: List<SeriesEpisode> = emptyList()
)
