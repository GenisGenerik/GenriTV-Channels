package com.example.genritv.observability

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class PlaybackTelemetry {
    private var startedAtMs = 0L
    private var bufferingStartedAtMs = 0L
    private var totalBufferingMs = 0L
    private var bufferingEvents = 0
    private var lastFormat: Format? = null

    fun onPlaybackStarted(nowMs: Long) {
        startedAtMs = nowMs
        bufferingStartedAtMs = 0L
        totalBufferingMs = 0L
        bufferingEvents = 0
        lastFormat = null
    }

    fun onBufferingStarted(nowMs: Long) {
        if (bufferingStartedAtMs == 0L) {
            bufferingStartedAtMs = nowMs
            bufferingEvents++
        }
    }

    fun onBufferingEnded(nowMs: Long) {
        if (bufferingStartedAtMs != 0L) {
            totalBufferingMs += (nowMs - bufferingStartedAtMs).coerceAtLeast(0L)
            bufferingStartedAtMs = 0L
        }
    }

    fun onVideoFormatChanged(format: Format) {
        lastFormat = format
    }

    fun onPlaybackReady(nowMs: Long) {
        val startupMs = if (startedAtMs > 0L) (nowMs - startedAtMs).coerceAtLeast(0L) else 0L
        Log.d(TAG, "ready startup_ms=$startupMs buffer_events=$bufferingEvents buffer_ms=$totalBufferingMs resolution=${lastFormat?.width}x${lastFormat?.height} bitrate=${lastFormat?.bitrate}")
    }

    fun onPlaybackError(errorCode: Int, message: String?) {
        Log.e(TAG, "error code=$errorCode message=${message.orEmpty().take(160)}")
    }

    companion object {
        private const val TAG = "GENRI_TV_PLAYBACK"
    }
}
