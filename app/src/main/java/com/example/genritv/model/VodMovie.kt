package com.example.genritv.model

data class VodMovie(
    val title: String,
    val url: String,
    val logo: String? = null,
    val genre: String? = null,
    val year: String? = null
)
