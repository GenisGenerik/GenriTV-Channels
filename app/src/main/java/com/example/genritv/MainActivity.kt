package com.example.genritv

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.compose.rememberNavController
import com.example.genritv.data.UnifiedChannelRepository
import com.example.genritv.model.TvChannel
import com.example.genritv.ui.PlayerScreen
import com.example.genritv.ui.PlayerViewModel
import com.example.genritv.ui.TVNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val PREFS_NAME = "genri_tv_prefs"
        private const val KEY_LAST_CHANNEL = "last_channel"
    }

    enum class AppMode { CHANNELS, VOD, SERIES }

    private var currentRoute by mutableStateOf("home")
    private var channels by mutableStateOf<List<TvChannel>>(emptyList())
    private var currentChannelName by mutableStateOf("")
    private var currentChannelLogo by mutableStateOf<String?>(null)
    private var currentMode by mutableStateOf(AppMode.CHANNELS)
    private var currentChannelIndex by mutableStateOf(0)
    private var showChannelName by mutableStateOf(false)
    private var hideChannelJob: Job? = null
    private var resizeMode by mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val loadedChannels = UnifiedChannelRepository.loadChannels(this@MainActivity)
                channels = loadedChannels
                currentChannelIndex = loadLastChannel()
                Log.d("GENRI_TV", "Loaded ${loadedChannels.size} channels")
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Fatal channel load error", e)
            }
        }

        setContent {
            val playerViewModel: PlayerViewModel = viewModel()
            val navController = rememberNavController()
            val player = playerViewModel.player
            val playerState by playerViewModel.playerState.collectAsStateCompat()
            val isPlayerPlaying = player.isPlaying
            val playerPosition = player.currentPosition.coerceAtLeast(0L)
            val playerDuration = player.duration.coerceAtLeast(0L)

            BackHandler(enabled = true) {
                if (currentRoute == "player") {
                    player.stop()
                    navController.popBackStack("home", inclusive = false)
                } else {
                    playerViewModel.releasePlayer()
                    finishAndRemoveTask()
                }
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                currentRoute = destination.route ?: "home"
                if (currentRoute != "player") {
                    player.stop()
                }
            }

            TVNavigation(
                navController = navController,
                onNavigateToLiveTv = { channel ->
                    currentMode = AppMode.CHANNELS
                    val index = channels.indexOf(channel)
                    if (index >= 0) {
                        currentChannelIndex = index
                        saveCurrentChannel()
                        currentChannelName = channel.nama
                        currentChannelLogo = channel.logo
                        showChannelName = true
                        resetHideJob(3000)
                        playerViewModel.playChannel(channel, 0, isVod = false)
                    }
                    navController.navigate("player")
                },
                onNavigateToMovies = {
                    currentMode = AppMode.VOD
                    navController.navigate("player")
                },
                onNavigateToSeries = {
                    currentMode = AppMode.SERIES
                    navController.navigate("player")
                },
                playerScreen = {
                    PlayerScreen(
                        player = player,
                        channelName = currentChannelName,
                        channelLogo = currentChannelLogo,
                        showChannelName = showChannelName,
                        isLoading = playerState is com.example.genritv.ui.PlayerState.Buffering,
                        currentTime = playerPosition,
                        showError = playerState is com.example.genritv.ui.PlayerState.Error,
                        currentMode = currentMode,
                        isPlaying = isPlayerPlaying,
                        position = playerPosition,
                        duration = playerDuration,
                        resizeMode = resizeMode,
                        onRetry = {
                            if (currentMode == AppMode.CHANNELS) {
                                playerViewModel.retryPlayback()
                            }
                        }
                    )
                }
            )
        }
    }

    private fun saveCurrentChannel() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_CHANNEL, currentChannelIndex)
            .apply()
    }

    private fun loadLastChannel(): Int {
        if (channels.isEmpty()) return 0
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_LAST_CHANNEL, 0)
            .coerceIn(0, channels.lastIndex)
    }

    private fun resetHideJob(delayMs: Long) {
        hideChannelJob?.cancel()
        hideChannelJob = lifecycleScope.launch {
            delay(delayMs)
            showChannelName = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (currentRoute != "player" || channels.isEmpty()) {
            return super.onKeyDown(keyCode, event)
        }

        return when (currentMode) {
            AppMode.CHANNELS -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    changeChannel(-1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    changeChannel(1)
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
            AppMode.VOD, AppMode.SERIES -> super.onKeyDown(keyCode, event)
        }
    }

    private fun changeChannel(delta: Int) {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + delta).mod(channels.size)
        val channel = channels[currentChannelIndex]
        currentChannelName = channel.nama
        currentChannelLogo = channel.logo
        showChannelName = true
        resetHideJob(3000)
        saveCurrentChannel()
        pendingChannelPlay = true
    }

    private var pendingChannelPlay by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        pendingChannelPlay = false
    }

    override fun onDestroy() {
        hideChannelJob?.cancel()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    androidx.lifecycle.compose.collectAsStateWithLifecycle(this)
