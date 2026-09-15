package com.example.genritv.data

import android.content.Context
import com.example.genritv.model.TvChannel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonHelper {

    fun loadChannels(context: Context): List<TvChannel> {

        val json = context.assets
            .open("channels.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<TvChannel>>() {}.type

        return Gson().fromJson(json, type)
    }
}