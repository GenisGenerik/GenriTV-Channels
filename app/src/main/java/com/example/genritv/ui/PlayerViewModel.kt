package com.example.genritv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.genritv.data.StreamUrlValidator
import com.example.genritv.model.TvChannel
import com.example.genritv.observability.PlaybackTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class PlayerViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val telemetry = PlaybackTelemetry()
    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() = _player ?: createPlayer().also { _player = it }

    private var currentChannel: TvChannel? = null
    private var currentUrlIndex = -1
    private var retryCount = 0

    private fun createPlayer(): ExoPlayer {
        val trackSelector = DefaultTrackSelector(application).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(false).build())
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                50_000,
                1_500,
                3_000
            )
            .build()

        return ExoPlayer.Builder(application)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val now = android.os.SystemClock.elapsedRealtime()
                        _playerState.value = when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                telemetry.onBufferingStarted(now)
                                PlayerState.Buffering
                            }
                            Player.STATE_READY -> {
                                telemetry.onBufferingEnded(now)
                                telemetry.onPlaybackReady(now)
                                PlayerState.Ready
                            }
                            Player.STATE_IDLE -> PlayerState.Idle
                            Player.STATE_ENDED -> PlayerState.Ended
                            else -> _playerState.value
                        }
                    }

                    override fun onVideoInputFormatChanged(format: androidx.media3.common.Format) {
                        telemetry.onVideoFormatChanged(format)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        telemetry.onPlaybackError(error.errorCode, error.message)
                        val fallbackIndex = nextPlayableUrlIndex(currentChannel, currentUrlIndex)
                        if (fallbackIndex != null && retryCount < MAX_AUTOMATIC_FALLBACKS) {
                            retryCount++
                            playChannel(currentChannel!!, fallbackIndex)
                        } else {
                            _playerState.value = PlayerState.Error(
                                error.message ?: "Pemutaran gagal"
                            )
                        }
                    }
                })
            }
    }

    fun playChannel(channel: TvChannel, urlIndex: Int = 0) {
        val playableUrls = channel.urls.filter(StreamUrlValidator::isPlayableHttpUrl)
        if (playableUrls.isEmpty()) {
            currentChannel = channel
            currentUrlIndex = -1
            _playerState.value = PlayerState.Error("Channel tidak memiliki URL stream yang valid")
            return
        }

        if (urlIndex !in playableUrls.indices) {
            _playerState.value = PlayerState.Error("Tidak ada sumber stream yang dapat diputar")
            return
        }

        currentChannel = channel.copy(urls = playableUrls)
        currentUrlIndex = urlIndex
        retryCount = 0
        telemetry.onPlaybackStarted(android.os.SystemClock.elapsedRealtime())

        val mediaItem = MediaItem.Builder()
            .setUri(playableUrls[urlIndex])
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        _playerState.value = PlayerState.Buffering
    }

    fun retryPlayback(): Boolean {
        val channel = currentChannel ?: return false
        val nextIndex = nextPlayableUrlIndex(channel, currentUrlIndex)
        if (nextIndex == null) {
            _playerState.value = PlayerState.Error("Semua sumber stream gagal")
            return false
        }

        retryCount = 0
        playChannel(channel, nextIndex)
        return true
    }

    private fun nextPlayableUrlIndex(channel: TvChannel?, currentIndex: Int): Int? {
        val urls = channel?.urls.orEmpty().filter(StreamUrlValidator::isPlayableHttpUrl)
        return urls.indices.firstOrNull { it > currentIndex }
    }

    fun releasePlayer() {
        _player?.release()
        _player = null
        currentChannel = null
        currentUrlIndex = -1
        retryCount = 0
        _playerState.value = PlayerState.Idle
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }

    companion object {
        private const val MAX_AUTOMATIC_FALLBACKS = 2
    }
}

sealed class PlayerState {
    data object Idle : PlayerState()
    data object Buffering : PlayerState()
    data object Ready : PlayerState()
    data object Ended : PlayerState()
    data class Error(val message: String) : PlayerState()
}
