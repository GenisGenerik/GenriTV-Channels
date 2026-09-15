package com.example.genritv.model

data class EpgProgram(
    val tvgId: String,
    val title: String,
    val startTime: Long, // Epoch millis
    val endTime: Long,
    val description: String? = null
)
