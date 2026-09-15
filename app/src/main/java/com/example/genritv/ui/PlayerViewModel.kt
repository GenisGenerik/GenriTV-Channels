package com.example.genritv.ui

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.genritv.data.StreamUrlValidator
import com.example.genritv.model.Series
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
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
                5_000,
                20_000,
                750,
                1_500
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
                            else -> _playerState.value
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        telemetry.onPlaybackError(error.errorCode, error.message)
                        val fallbackIndex = nextPlayableUrlIndex(currentChannel, currentUrlIndex)
                        if (fallbackIndex != null && retryCount < MAX_AUTOMATIC_FALLBACKS) {
                            retryCount++
                            playCurrentSource(fallbackIndex)
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
        playSources(channel.urls, channelLabel = channel.nama, urlIndex = urlIndex) {
            currentChannel = channel.copy(urls = it)
        }
    }

    fun playMovie(movie: VodMovie) {
        currentChannel = null
        playSources(listOf(movie.url), channelLabel = movie.title)
    }

    fun playSeries(series: Series) {
        val episode = series.episodes.firstOrNull { StreamUrlValidator.isPlayableHttpUrl(it.url) }
        if (episode == null) {
            _playerState.value = PlayerState.Error("Series belum memiliki episode dengan sumber video yang valid")
            return
        }
        currentChannel = null
        playSources(listOf(episode.url), channelLabel = "${series.title} • ${episode.title}")
    }

    private fun playSources(
        sourceUrls: List<String>,
        channelLabel: String,
        urlIndex: Int = 0,
        onValidated: (List<String>) -> Unit = {}
    ) {
        val playableUrls = sourceUrls.filter(StreamUrlValidator::isPlayableHttpUrl)
        if (playableUrls.isEmpty()) {
            _playerState.value = PlayerState.Error("$channelLabel belum memiliki sumber video yang valid")
            return
        }

        if (urlIndex !in playableUrls.indices) {
            _playerState.value = PlayerState.Error("Tidak ada sumber stream yang dapat diputar")
            return
        }

        onValidated(playableUrls)
        currentUrlIndex = urlIndex
        retryCount = 0
        telemetry.onPlaybackStarted(android.os.SystemClock.elapsedRealtime())
        playCurrentSource(urlIndex)
    }

    private fun playCurrentSource(urlIndex: Int) {
        val urls = currentChannel?.urls ?: currentPlayableUrlsFromCurrentMedia()
        if (urlIndex !in urls.indices) {
            _playerState.value = PlayerState.Error("Sumber video tidak tersedia")
            return
        }

        currentUrlIndex = urlIndex
        try {
            player.setMediaItem(MediaItem.fromUri(urls[urlIndex]))
            player.prepare()
            player.playWhenReady = true
            _playerState.value = PlayerState.Buffering
        } catch (e: RuntimeException) {
            telemetry.onPlaybackError(PlaybackException.ERROR_CODE_UNSPECIFIED, e.message)
            _playerState.value = PlayerState.Error(
                e.message ?: "Format stream tidak didukung"
            )
        }
    }

    private fun currentPlayableUrlsFromCurrentMedia(): List<String> =
        player.currentMediaItem?.localConfiguration?.uri?.toString()?.let(::listOf).orEmpty()

    fun retryPlayback(): Boolean {
        val nextIndex = nextPlayableUrlIndex(currentChannel, currentUrlIndex)
        if (nextIndex != null) {
            retryCount = 0
            playCurrentSource(nextIndex)
            return true
        }

        val currentUrl = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUrl != null && StreamUrlValidator.isPlayableHttpUrl(currentUrl)) {
            retryCount = 0
            telemetry.onPlaybackStarted(android.os.SystemClock.elapsedRealtime())
            player.prepare()
            player.playWhenReady = true
            _playerState.value = PlayerState.Buffering
            return true
        }

        _playerState.value = PlayerState.Error("Semua sumber stream gagal")
        return false
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
    data class Error(val message: String) : PlayerState()
}
