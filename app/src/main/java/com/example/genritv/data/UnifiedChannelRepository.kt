package com.example.genritv.data

import android.content.Context
import android.util.Log
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.TimeUnit

object UnifiedChannelRepository {

    private const val CHANNELS_JSON_URL =
        "https://raw.githubusercontent.com/GenisGenerik/GenriTV/main/generated/channels.json"
    private const val CACHE_FILE_NAME = "channels_cache.json"
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun loadChannels(context: Context): List<TvChannel> {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GENRI_TV", "Fetching channels from: $CHANNELS_JSON_URL")
                
                val request = Request.Builder().url(CHANNELS_JSON_URL).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")
                    
                    val json = response.body?.string() ?: throw Exception("Empty body")
                    val type = object : TypeToken<List<TvChannel>>() {}.type
                    val channels: List<TvChannel> = gson.fromJson(json, type)
                    
                    saveToCache(cacheFile, channels)
                    channels
                }
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Failed to fetch channels, trying cache", e)
                loadFromCache(cacheFile)
            }
        }
    }

    private fun saveToCache(cacheFile: File, channels: List<TvChannel>) {
        try {
            FileWriter(cacheFile).use { writer ->
                gson.toJson(channels, writer)
            }
        } catch (e: Exception) {
            Log.e("GENRI_TV", "Failed to save cache", e)
        }
    }

    private fun loadFromCache(cacheFile: File): List<TvChannel> {
        return try {
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
