package com.example.genritv.data

import android.content.Context
import android.util.Log
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URL

object M3uRepository {

    // Premium Indonesian Sources
    private val M3U_SOURCES = listOf(
        "https://iptv-org.github.io/iptv/countries/id.m3u",
        "https://raw.githubusercontent.com/riotryulianto/iptv-playlists/master/indonesia.m3u",
        "https://raw.githubusercontent.com/dhasap/dhanytv/main/dhanytv.m3u"
    )
    
    private const val CACHE_FILE_NAME = "premium_channels_v3.json"
    private val gson = Gson()

    suspend fun loadChannels(context: Context): List<TvChannel> {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        
        return try {
            val allParsedChannels = mutableListOf<TvChannel>()
            
            withContext(Dispatchers.IO) {
                M3U_SOURCES.forEach { sourceUrl ->
                    try {
                        val content = URL(sourceUrl).readText()
                        allParsedChannels.addAll(M3uParser.parse(content))
                    } catch (e: Exception) {
                        Log.e("GENRI_TV", "Failed to load source: $sourceUrl", e)
                    }
                }
            }
            
            if (allParsedChannels.isEmpty()) {
                val cached = loadFromCache(cacheFile)
                if (cached.isNotEmpty()) return cached
            }

            // Merging logic
            val mergedChannels = mergeChannels(allParsedChannels)
            
            // Save to cache
            withContext(Dispatchers.IO) {
                try {
                    FileWriter(cacheFile).use { writer ->
                        gson.toJson(mergedChannels, writer)
                    }
                } catch (e: Exception) {
                    Log.e("GENRI_TV", "Failed to save cache", e)
                }
            }
            
            Log.d("GENRI_TV", "Merged ${mergedChannels.size} Indonesian channels")
            mergedChannels
            
        } catch (e: Exception) {
            Log.e("GENRI_TV", "Critical failure loading channels, trying cache", e)
            loadFromCache(cacheFile)
        }
    }

    private fun mergeChannels(channels: List<TvChannel>): List<TvChannel> {
        return channels
            .groupBy { cleanChannelName(it.nama) }
            .map { (_, group) ->
                val primary = group.first()
                // Take all unique URLs, limit to 3
                val allUrls = group.flatMap { it.urls }.distinct().take(3)
                TvChannel(
                    nama = primary.nama,
                    urls = allUrls,
                    logo = group.firstOrNull { !it.logo.isNullOrEmpty() }?.logo,
                    grup = group.firstOrNull { !it.grup.isNullOrEmpty() }?.grup,
                    tvgId = group.firstOrNull { !it.tvgId.isNullOrEmpty() }?.tvgId
                )
            }
            .filter { it.urls.isNotEmpty() }
            .sortedByDescending { isNational(it.grup, it.nama) }
            .take(200) // Keep it focused as requested
    }

    private fun cleanChannelName(name: String): String {
        return name.uppercase()
            .replace("INDONESIA", "")
            .replace("IDN", "")
            .replace("HD", "")
            .replace("SD", "")
            .replace("CHANNEL", "")
            .replace("TV", "")
            .replace(Regex("[^A-Z0-9]"), "")
            .trim()
    }

    private fun isNational(grup: String?, nama: String?): Boolean {
        val g = grup?.lowercase() ?: ""
        val n = nama?.lowercase() ?: ""
        val nationalKeywords = listOf(
            "rcti", "sctv", "indosiar", "trans", "antv", "tvone", "metro", 
            "kompas", "mnc", "global", "gtv", "inews", "tvri", "rtv", "net"
        )
        return g.contains("nasional") || g.contains("indo") || 
               nationalKeywords.any { n.contains(it) }
    }

    private suspend fun loadFromCache(cacheFile: File): List<TvChannel> {
        return withContext(Dispatchers.IO) {
            try {
                if (cacheFile.exists()) {
                    FileReader(cacheFile).use { reader ->
                        val type = object : TypeToken<List<TvChannel>>() {}.type
                        gson.fromJson<List<TvChannel>>(reader, type) ?: emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Cache read failed", e)
                emptyList()
            }
        }
    }
}
