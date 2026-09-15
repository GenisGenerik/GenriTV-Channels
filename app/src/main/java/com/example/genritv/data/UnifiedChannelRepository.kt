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

    private const val TAG = "GENRI_TV"
    private const val CHANNELS_JSON_URL =
        "https://raw.githubusercontent.com/GenisGenerik/GenriTV-Channels/main/generated/channels.json"
    private const val CACHE_FILE_NAME = "channels_cache.json"
    private const val CACHE_TIMESTAMP_FILE_NAME = "channels_cache_timestamp"
    private const val MAX_ATTEMPTS = 3
    private const val MIN_CHANNELS = 1
    private const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun loadChannels(context: Context, forceRefresh: Boolean = false): List<TvChannel> =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            val timestampFile = File(context.filesDir, CACHE_TIMESTAMP_FILE_NAME)

            if (!forceRefresh && isCacheFresh(cacheFile, timestampFile)) {
                loadFromCache(cacheFile).takeIf { it.size >= MIN_CHANNELS }?.let {
                    Log.d(TAG, "Using fresh channel cache: ${it.size}")
                    return@withContext it
                }
            }

            val remoteChannels = fetchRemoteChannels()
            if (remoteChannels.size >= MIN_CHANNELS) {
                saveToCache(cacheFile, timestampFile, remoteChannels)
                return@withContext remoteChannels
            }

            val cachedChannels = loadFromCache(cacheFile)
            if (cachedChannels.size >= MIN_CHANNELS) {
                Log.w(TAG, "Using stale cached channels: ${cachedChannels.size}")
                return@withContext cachedChannels
            }

            try {
                JsonHelper.loadChannels(context).takeIf { it.size >= MIN_CHANNELS }?.also {
                    Log.w(TAG, "Using bundled channels: ${it.size}")
                    return@withContext it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bundled channel source failed", e)
            }

            emptyList()
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
                        ?.map(::normalizeChannel)
                        ?.filter { channel ->
                            channel.nama.isNotBlank() && channel.urls.isNotEmpty()
                        }
                        ?.distinctBy { it.tvgId?.takeIf(String::isNotBlank) ?: it.nama.lowercase() }
                        .orEmpty()

                    if (channels.isEmpty()) {
                        throw IllegalStateException("Remote channels.json contains no valid channels")
                    }

                    Log.d(TAG, "Loaded ${channels.size} channels from GitHub")
                    return channels
                }
            } catch (e: JsonSyntaxException) {
                lastError = e
                Log.e(TAG, "Invalid channels.json", e)
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Channel fetch attempt ${attempt + 1}/$MAX_ATTEMPTS failed: ${e.message}")
                if (attempt < MAX_ATTEMPTS - 1) delay(500L * (attempt + 1))
            }
        }

        Log.e(TAG, "Remote channel fetch failed after $MAX_ATTEMPTS attempts", lastError)
        return emptyList()
    }

    private fun normalizeChannel(channel: TvChannel): TvChannel = channel.copy(
        nama = channel.nama.trim(),
        urls = channel.urls.map(String::trim).filter(String::isNotBlank).distinct(),
        logo = channel.logo?.trim()?.takeIf(String::isNotBlank),
        grup = channel.grup?.trim()?.takeIf(String::isNotBlank),
        tvgId = channel.tvgId?.trim()?.takeIf(String::isNotBlank)
    )

    private fun saveToCache(cacheFile: File, timestampFile: File, channels: List<TvChannel>) {
        try {
            cacheFile.parentFile?.mkdirs()
            FileWriter(cacheFile).use { writer -> gson.toJson(channels, writer) }
            timestampFile.writeText(System.currentTimeMillis().toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save channel cache", e)
        }
    }

    private fun isCacheFresh(cacheFile: File, timestampFile: File): Boolean {
        if (!cacheFile.exists() || !timestampFile.exists()) return false
        val timestamp = timestampFile.readText().trim().toLongOrNull() ?: return false
        return System.currentTimeMillis() - timestamp in 0..CACHE_TTL_MS
    }

    private fun loadFromCache(cacheFile: File): List<TvChannel> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            FileReader(cacheFile).use { reader ->
                val type = object : TypeToken<List<TvChannel>>() {}.type
                gson.fromJson<List<TvChannel>>(reader, type)
                    ?.map(::normalizeChannel)
                    ?.filter { it.nama.isNotBlank() && it.urls.isNotEmpty() }
                    ?.distinctBy { it.tvgId?.takeIf(String::isNotBlank) ?: it.nama.lowercase() }
                    .orEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cache read failed", e)
            emptyList()
        }
    }
}
