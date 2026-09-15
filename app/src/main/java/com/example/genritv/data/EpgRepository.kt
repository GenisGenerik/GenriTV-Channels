package com.example.genritv.data

import com.example.genritv.model.EpgProgram
import java.util.Calendar

object EpgRepository {
    
    // Mock EPG data generator
    fun getCurrentProgram(tvgId: String?): EpgProgram? {
        if (tvgId == null) return null
        
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Simple mock: programs change every hour
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.HOUR, 1)
        val endTime = calendar.timeInMillis
        
        return EpgProgram(
            tvgId = tvgId,
            title = "Acara Saat Ini - $tvgId",
            startTime = startTime,
            endTime = endTime,
            description = "Deskripsi acara yang sedang berlangsung..."
        )
    }

    fun getNextProgram(tvgId: String?): EpgProgram? {
        if (tvgId == null) return null
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        calendar.add(Calendar.HOUR, 1)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.HOUR, 1)
        val endTime = calendar.timeInMillis
        
        return EpgProgram(
            tvgId = tvgId,
            title = "Acara Berikutnya - $tvgId",
            startTime = startTime,
            endTime = endTime,
            description = "Deskripsi acara selanjutnya..."
        )
    }
}
