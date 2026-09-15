package com.example.genritv.model

data class TvChannel(
    val nama: String,
    val urls: List<String>,
    val logo: String? = null,
    val grup: String? = null,
    val tvgId: String? = null
) {
    val url: String get() = urls.firstOrNull().orEmpty()
    val hasPlayableUrl: Boolean get() = urls.any { it.isNotBlank() }
}
