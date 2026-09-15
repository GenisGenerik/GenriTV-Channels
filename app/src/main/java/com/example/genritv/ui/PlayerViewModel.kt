package com.example.genritv.ui

import com.example.genritv.model.TvChannel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(private val application: Application) : AndroidViewModel(application) {
    
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var _player: ExoPlayer? = null
    val player: ExoPlayer get() = _player ?: createPlayer(false).also { _player = it }

    private var currentChannel: TvChannel? = null
    private var currentUrlIndex = 0
    private var isVodMode: Boolean = false

    private fun createPlayer(isVod: Boolean): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferParameters(
                if (isVod) 30000 else 5000,    // minBufferMs
                if (isVod) 60000 else 15000,   // maxBufferMs
                2500,                          // bufferForPlaybackMs
                5000                           // bufferForPlaybackAfterRebufferMs
            )
            .build()
            
        return ExoPlayer.Builder(application)
            .setLoadControl(loadControl)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> _playerState.value = PlayerState.Buffering
                            Player.STATE_READY -> _playerState.value = PlayerState.Ready
                            Player.STATE_IDLE -> _playerState.value = PlayerState.Idle
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playerState.value = PlayerState.Error(error.message ?: "Unknown Error")
                    }
                })
            }
    }

    fun playChannel(channel: TvChannel, urlIndex: Int = 0, isVod: Boolean = false) {
        if (isVod != isVodMode || _player == null) {
            _player?.release()
            _player = createPlayer(isVod)
            isVodMode = isVod
        }
        
        currentChannel = channel
        currentUrlIndex = urlIndex
        
        if (urlIndex < channel.urls.size) {
            val url = channel.urls[urlIndex]
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else {
            _playerState.value = PlayerState.Error("No more URLs to try")
        }
    }


    fun retryPlayback() {
        val channel = currentChannel ?: return
        currentUrlIndex++
        playChannel(channel, currentUrlIndex)
    }

    fun releasePlayer() {
        player.release()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}

sealed class PlayerState {
    object Idle : PlayerState()
    object Buffering : PlayerState()
    object Ready : PlayerState()
    data class Error(val message: String) : PlayerState()
}
