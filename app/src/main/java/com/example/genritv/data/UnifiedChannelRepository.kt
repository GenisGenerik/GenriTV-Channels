package com.example.genritv.data

import android.content.Context
import android.util.Log
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
    private const val MIN_CHANNELS = 1

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadChannels(context: Context): List<TvChannel> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

        val remoteChannels = fetchRemoteChannels()
        if (remoteChannels.size >= MIN_CHANNELS) {
            saveToCache(cacheFile, remoteChannels)
            return@withContext remoteChannels
        }

        val cachedChannels = loadFromCache(cacheFile)
        if (cachedChannels.size >= MIN_CHANNELS) {
            Log.w("GENRI_TV", "Using cached channels: ${cachedChannels.size}")
            return@withContext cachedChannels
        }

        try {
            val bundled = JsonHelper.loadChannels(context)
            if (bundled.isNotEmpty()) {
                Log.w("GENRI_TV", "Using bundled channels: ${bundled.size}")
                bundled
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("GENRI_TV", "No channel source available", e)
            emptyList()
        }
    }

    private suspend fun fetchRemoteChannels(): List<TvChannel> {
        var lastError: Exception? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = Request.Builder()
                    .url(CHANNELS_JSON_URL)
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }

                    val json = response.body?.string().orEmpty()
                    if (json.isBlank()) throw IllegalStateException("Empty response body")

                    val type = object : TypeToken<List<TvChannel>>() {}.type
                    val channels = gson.fromJson<List<TvChannel>>(json, type)
                        ?.filter { channel ->
                            channel.nama.isNotBlank() &&
                                channel.urls.any { url -> url.isNotBlank() }
                        }
                        .orEmpty()

                    if (channels.isEmpty()) {
                        throw IllegalStateException("Remote channels.json contains no valid channels")
                    }

                    Log.d("GENRI_TV", "Loaded ${channels.size} channels from GitHub")
                    return channels
                }
            } catch (e: JsonSyntaxException) {
                lastError = e
                Log.e("GENRI_TV", "Invalid channels.json", e)
            } catch (e: Exception) {
                lastError = e
                Log.w(
                    "GENRI_TV",
                    "Channel fetch attempt ${attempt + 1}/$MAX_ATTEMPTS failed: ${e.message}"
                )
                if (attempt < MAX_ATTEMPTS - 1) delay(500L * (attempt + 1))
            }
        }

        Log.e("GENRI_TV", "Remote channel fetch failed after $MAX_ATTEMPTS attempts", lastError)
        return emptyList()
    }

    private fun saveToCache(cacheFile: File, channels: List<TvChannel>) {
        try {
            cacheFile.parentFile?.mkdirs()
            FileWriter(cacheFile).use { writer ->
                gson.toJson(channels, writer)
            }
        } catch (e: Exception) {
            Log.e("GENRI_TV", "Failed to save channel cache", e)
        }
    }

    private fun loadFromCache(cacheFile: File): List<TvChannel> {
        return try {
            if (!cacheFile.exists()) return emptyList()

            FileReader(cacheFile).use { reader ->
                val type = object : TypeToken<List<TvChannel>>() {}.type
                gson.fromJson<List<TvChannel>>(reader, type)
                    ?.filter { channel ->
                        channel.nama.isNotBlank() &&
                            channel.urls.any { url -> url.isNotBlank() }
                    }
                    .orEmpty()
            }
        } catch (e: Exception) {
            Log.e("GENRI_TV", "Cache read failed", e)
            emptyList()
        }
    }
}
