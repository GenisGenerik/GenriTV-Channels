package com.example.genritv.data

import android.content.Context
import android.util.Log
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.TimeUnit

object UnifiedChannelRepository {

    private const val CHANNELS_JSON_URL =
        "https://raw.githubusercontent.com/GenisGenerik/GenriTV-Channels/main/generated/channels.json"
    private const val CACHE_FILE_NAME = "channels_cache.json"
    private const val MAX_ATTEMPTS = 3

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun loadChannels(context: Context): List<TvChannel> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)

        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                Log.d("GENRI_TV", "Fetching channels (attempt $attempt/$MAX_ATTEMPTS): $CHANNELS_JSON_URL")

                val request = Request.Builder()
                    .url(CHANNELS_JSON_URL)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }

                    val json = response.body?.string().orEmpty()
                    if (json.isBlank()) {
                        throw IllegalStateException("Empty channels response")
                    }

                    val type = object : TypeToken<List<TvChannel>>() {}.type
                    val channels: List<TvChannel> = gson.fromJson(json, type)
                        ?: throw JsonParseException("Invalid channels JSON")

                    val validChannels = channels.filter { it.nama.isNotBlank() && it.urls.isNotEmpty() }
                    if (validChannels.isEmpty()) {
                        throw IllegalStateException("Channels response contains no valid channels")
                    }

                    saveToCache(cacheFile, validChannels)
                    Log.d("GENRI_TV", "Loaded ${validChannels.size} channels from GitHub")
                    return@withContext validChannels
                }
            } catch (e: Exception) {
                Log.w("GENRI_TV", "Channel fetch attempt $attempt failed: ${e.message}")
                if (attempt < MAX_ATTEMPTS) {
                    delay((attempt * 1000L).coerceAtMost(3000L))
                }
            }
        }

        val cached = loadFromCache(cacheFile)
        if (cached.isNotEmpty()) {
            Log.d("GENRI_TV", "Using ${cached.size} cached channels")
            return@withContext cached
        }

        // Last-resort bundled fallback so the app still has data on first launch/offline.
        return@withContext try {
            val bundled = JsonHelper.loadChannels(context)
                .filter { it.nama.isNotBlank() && it.urls.isNotEmpty() }
            Log.d("GENRI_TV", "Using ${bundled.size} bundled fallback channels")
            bundled
        } catch (e: Exception) {
            Log.e("GENRI_TV", "No channel source available", e)
            emptyList()
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
            if (!cacheFile.exists()) return emptyList()

            FileReader(cacheFile).use { reader ->
                val type = object : TypeToken<List<TvChannel>>() {}.type
                gson.fromJson<List<TvChannel>>(reader, type)
                    ?.filter { it.nama.isNotBlank() && it.urls.isNotEmpty() }
                    ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("GENRI_TV", "Cache read failed", e)
            emptyList()
        }
    }
}
