package com.example.genritv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.genritv.ui.PlayerScreen
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.genritv.data.UnifiedChannelRepository
import com.example.genritv.model.TvChannel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genritv.ui.PlayerViewModel
import com.example.genritv.ui.TVNavigation

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    
    private var currentRoute by mutableStateOf("home")
    private var channels by mutableStateOf<List<TvChannel>>(emptyList())
    private var currentChannelName by mutableStateOf("")
    private var currentChannelLogo by mutableStateOf<String?>(null)
    private var showError by mutableStateOf(false)
    
    // New navigation states
    enum class AppMode { CHANNELS, VOD, SERIES }
    private var currentMode by mutableStateOf(AppMode.CHANNELS)
    private var currentChannelIndex by mutableStateOf(0)

    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            playerViewModel = viewModel()
            val navController = rememberNavController()
            
            BackHandler(enabled = true) {
                if (currentRoute == "player") {
                    navController.popBackStack("home", inclusive = false)
                } else {
                    playerViewModel.releasePlayer()
                    finishAndRemoveTask()
                }
            }

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
                onNavigateToMovies = { _ ->
                    currentMode = AppMode.VOD
                    navController.navigate("player")
                },
                onNavigateToSeries = { _ ->
                    currentMode = AppMode.SERIES
                    navController.navigate("player")
                },
                playerScreen = {
                    PlayerScreen(
                        player = playerViewModel.player,
                        channelName = currentChannelName,
                        channelLogo = currentChannelLogo,
                        showChannelName = false,
                        isLoading = false,
                        currentTime = "",
                        showError = showError,
                        currentMode = currentMode,
                        isPlaying = playerViewModel.player.isPlaying,
                        position = playerViewModel.player.currentPosition,
                        duration = playerViewModel.player.duration,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        onRetry = { playerViewModel.retryPlayback() }
                    )
                }
            )
        }

        lifecycleScope.launch {
            try {
                val loadedChannels = UnifiedChannelRepository.loadChannels(this@MainActivity)
                if (loadedChannels.isNotEmpty()) {
                    channels = loadedChannels
                } else {
                    showError = true
                    currentChannelName = "Tidak ada channel tersedia"
                }
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Fatal error during startup", e)
                showError = true
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
