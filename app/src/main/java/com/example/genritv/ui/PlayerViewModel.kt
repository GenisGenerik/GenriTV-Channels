package com.example.genritv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.genritv.model.TvChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer(false).also { _player = it }

    private var currentChannel: TvChannel? = null
    private var currentUrlIndex = 0
    private var isVodMode = false

    private fun createPlayer(isVod: Boolean): ExoPlayer {
        val loadControl = if (isVod) {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 60_000, 2_500, 5_000)
                .build()
        } else {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(5_000, 15_000, 1_500, 2_500)
                .build()
        }

        return ExoPlayer.Builder(application)
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playerState.value = when (playbackState) {
                            Player.STATE_BUFFERING -> PlayerState.Buffering
                            Player.STATE_READY -> PlayerState.Ready
                            Player.STATE_IDLE -> PlayerState.Idle
                            else -> _playerState.value
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playerState.value = PlayerState.Error(error.message ?: "Unknown Error")
                    }
                })
            }
    }

    fun playChannel(channel: TvChannel, urlIndex: Int = 0, isVod: Boolean = false) {
        if (urlIndex !in channel.urls.indices) {
            _playerState.value = PlayerState.Error("No more stream URLs available")
            return
        }

        if (isVod != isVodMode || _player == null) {
            _player?.release()
            _player = createPlayer(isVod)
            isVodMode = isVod
        }

        currentChannel = channel
        currentUrlIndex = urlIndex

        val mediaItem = MediaItem.fromUri(channel.urls[urlIndex])
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        _playerState.value = PlayerState.Buffering
    }

    fun retryPlayback(): Boolean {
        val channel = currentChannel ?: return false
        val nextIndex = currentUrlIndex + 1
        if (nextIndex !in channel.urls.indices) {
            _playerState.value = PlayerState.Error("All stream URLs failed")
            return false
        }

        playChannel(channel, nextIndex, isVodMode)
        return true
    }

    fun playFallback(channel: TvChannel): Boolean {
        if (channel.urls.isEmpty()) {
            _playerState.value = PlayerState.Error("No stream URLs available")
            return false
        }
        currentUrlIndex = 0
        playChannel(channel, 0, isVodMode)
        return true
    }

    fun releasePlayer() {
        _player?.release()
        _player = null
        currentChannel = null
        currentUrlIndex = 0
        _playerState.value = PlayerState.Idle
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}

sealed class PlayerState {
    data object Idle : PlayerState()
    data object Buffering : PlayerState()
    data object Ready : PlayerState()
    data class Error(val message: String) : PlayerState()
}
