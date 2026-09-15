package com.example.genritv.data

import android.content.Context
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object GithubChannelRepository {

    // GANTI DENGAN RAW URL MILIKMU NANTI
    private const val CHANNELS_URL =
        "https://raw.githubusercontent.com/GenisGenerik/GenriTV-Channels/main/channels.json"

    suspend fun loadChannels(
        context: Context
    ): List<TvChannel> {

        return try {

            val json = withContext(Dispatchers.IO) {

                URL(CHANNELS_URL)
                    .readText()
            }

            val type =
                object : TypeToken<List<TvChannel>>() {}.type

            android.util.Log.d(
                "GENRI_TV",
                "Playlist dari GitHub"
            )

            Gson().fromJson(json, type)

        } catch (e: Exception) {

            android.util.Log.d(
                "GENRI_TV",
                "Fallback ke assets"
            )

            JsonHelper.loadChannels(context)
        }
    }
}