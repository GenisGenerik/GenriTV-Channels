package com.example.genritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.genritv.ui.HomeScreen
import com.example.genritv.ui.PlayerScreen

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import android.util.Log
import android.view.KeyEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.example.genritv.data.UnifiedChannelRepository
import com.example.genritv.model.Series
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genritv.ui.PlayerViewModel

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    companion object {
        private const val PREFS_NAME = "genri_tv_prefs"
        private const val KEY_LAST_CHANNEL = "last_channel"
    }
    // player instance removed
    private var currentRoute by mutableStateOf("home")
    private var channels by mutableStateOf<List<TvChannel>>(emptyList())
    private var currentChannelName by mutableStateOf("")
    private var currentChannelLogo by mutableStateOf<String?>(null)
    private var showChannelName by mutableStateOf(false)
    private var hideChannelJob: Job? = null

    // New navigation states
    enum class AppMode { CHANNELS, VOD, SERIES }
    private var currentMode by mutableStateOf(AppMode.CHANNELS)
    private var currentChannelIndex by mutableStateOf(0)
    private var currentUrlIndex by mutableStateOf(0)

    private var playerPosition by mutableStateOf(0L)
    private var playerDuration by mutableStateOf(0L)
    private var isPlayerPlaying by mutableStateOf(false)

    private var resizeMode by mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)

    // ... (rest of class)

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    playerViewModel.player.prepare()
                    playerViewModel.player.play()
                }
        startClock()
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {
                    isLoading =
                        playbackState ==
                                Player.STATE_BUFFERING
                    playerDuration = player.duration.coerceAtLeast(0)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayerPlaying = isPlaying
                }
            }
        )

        lifecycleScope.launch {
            while(true) {
                if (currentRoute == "player") {
                    playerPosition = player.currentPosition.coerceAtLeast(0)
                }
                delay(1000)
            }
        }

import com.example.genritv.ui.TVNavigation

// ... inside setContent
            val playerViewModel: PlayerViewModel = viewModel()
            val navController = rememberNavController()
            
            // Handle navigasi tombol Back sesuai standar TV
            BackHandler(enabled = true) {
                if (currentRoute == "player") {
                    navController.popBackStack("home", inclusive = false)
                } else {
                    playerViewModel.releasePlayer()
                    finishAndRemoveTask()
                }
            }

            // Sync currentRoute & player stop logic
            navController.addOnDestinationChangedListener { _, destination, _ ->
                currentRoute = destination.route ?: "home"
                if (currentRoute != "player") {
                    playerViewModel.player.stop()
                }
            }

            TVNavigation(
                navController = navController,
                onNavigateToLiveTv = { channel ->
                    currentMode = AppMode.CHANNELS
                    playerViewModel.playChannel(channel, isVod = false)
                    navController.navigate("player")
                },
                onNavigateToMovies = { movie ->
                    // playVod(movie) // Needs migration
                    currentMode = AppMode.VOD
                    navController.navigate("player")
                },
                onNavigateToSeries = { series ->
                    // playSeries(series) // Needs migration
                    currentMode = AppMode.SERIES
                    navController.navigate("player")
                },
                playerScreen = {
                    PlayerScreen(
                        player = playerViewModel.player,
                        channelName = currentChannelName,
                        channelLogo = currentChannelLogo,
                        showChannelName = showChannelName,
                        isLoading = false,
                        currentTime = 0L,
                        showError = false,
                        currentMode = currentMode,
                        isPlaying = isPlayerPlaying,
                        position = playerPosition,
                        duration = playerDuration,
                        resizeMode = resizeMode,
                        onRetry = { playerViewModel.retryPlayback() }
                    )
                }
            )

        lifecycleScope.launch {
            try {
                val loadedChannels = UnifiedChannelRepository.loadChannels(this@MainActivity)
                Log.d("GENRI_TV", "Jumlah channel: ${loadedChannels.size}")

                if (loadedChannels.isNotEmpty()) {
                    channels = loadedChannels
                } else {
                    showError = true
                    currentChannelName = "Tidak ada channel tersedia"
                }
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Fatal error during startup", e)
                showError = true
                currentChannelName = "Kesalahan Koneksi"
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (currentRoute == "player" && !playerViewModel.player.isPlaying) {
            playerViewModel.player.play()
        }
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.player.pause()
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.player.stop()
    }

    private fun playChannel(channelIndex: Int, urlIndex: Int) {
        currentChannelIndex = channelIndex
        currentUrlIndex = urlIndex
        val channel = channels.getOrNull(channelIndex) ?: return
        currentChannelName = channel.nama
        currentChannelLogo = channel.logo
        playerViewModel.playChannel(channel, urlIndex, currentMode != AppMode.CHANNELS)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerViewModel.releasePlayer()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleRemoteKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleRemoteKey(keyCode: Int): Boolean {
        if (currentRoute != "player") return false

        return when (currentMode) {
            AppMode.CHANNELS -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        currentChannelIndex = (currentChannelIndex - 1 + channels.size) % channels.size
                        playChannel(currentChannelIndex, 0)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        currentChannelIndex = (currentChannelIndex + 1) % channels.size
                        playChannel(currentChannelIndex, 0)
                        true
                    }
                    else -> false
                }
            }
            AppMode.VOD, AppMode.SERIES -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        if (playerViewModel.player.isPlaying) playerViewModel.player.pause() else playerViewModel.player.play()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        playerViewModel.player.seekTo(playerViewModel.player.currentPosition - 10000)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        playerViewModel.player.seekTo(playerViewModel.player.currentPosition + 10000)
                        true
                    }
                    else -> false
                }
            }
        }
    }
}

