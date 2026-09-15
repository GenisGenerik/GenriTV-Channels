package com.example.genritv

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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

    private lateinit var playerViewModel: PlayerViewModel
    private var currentRoute by mutableStateOf("home")
    private var channels by mutableStateOf<List<TvChannel>>(emptyList())
    private var currentChannelName by mutableStateOf("")
    private var currentChannelLogo by mutableStateOf<String?>(null)
    private var currentMode by mutableStateOf(AppMode.CHANNELS)
    private var currentChannelIndex by mutableStateOf(0)
    private var showChannelName by mutableStateOf(false)
    private var hideChannelJob: Job? = null
    private var resizeMode by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        lifecycleScope.launch {
            try {
                val loadedChannels = UnifiedChannelRepository.loadChannels(this@MainActivity)
                channels = loadedChannels
                if (loadedChannels.isNotEmpty()) {
                    currentChannelIndex = loadLastChannel()
                    Log.d("GENRI_TV", "Loaded ${loadedChannels.size} channels")
                } else {
                    Log.e("GENRI_TV", "No channels available from remote, cache, or assets")
                }
            } catch (e: Exception) {
                Log.e("GENRI_TV", "Fatal channel load error", e)
            }
        }

        setContent {
            val navController = rememberNavController()
            val player = playerViewModel.player
            val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

            LaunchedEffect(playerState) {
                // Force Compose to observe player-state changes so channel loading and errors recompose reliably.
            }

            DisposableEffect(navController) {
                val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                    currentRoute = destination.route ?: "home"
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose { navController.removeOnDestinationChangedListener(listener) }
            }

            TVNavigation(
                navController = navController,
                onNavigateToLiveTv = { channel ->
                    currentMode = AppMode.CHANNELS
                    playChannel(channel)
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
                        currentTime = player.currentPosition.coerceAtLeast(0L).toString(),
                        showError = playerState is com.example.genritv.ui.PlayerState.Error,
                        currentMode = currentMode,
                        isPlaying = player.isPlaying,
                        position = player.currentPosition.coerceAtLeast(0L),
                        duration = player.duration.coerceAtLeast(0L),
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

    private fun playChannel(channel: TvChannel) {
        val index = channels.indexOfFirst { it.nama == channel.nama && it.urls == channel.urls }
        if (index >= 0) currentChannelIndex = index
        saveCurrentChannel()
        currentChannelName = channel.nama
        currentChannelLogo = channel.logo
        showChannelName = true
        resetHideJob(3000)
        playerViewModel.playChannel(channel)
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

    private fun changeChannel(delta: Int) {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + delta).mod(channels.size)
        playChannel(channels[currentChannelIndex])
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (currentRoute != "player" || currentMode != AppMode.CHANNELS || channels.isEmpty()) {
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
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
    }

    override fun onPause() {
        super.onPause()
        if (::playerViewModel.isInitialized) {
            playerViewModel.player.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::playerViewModel.isInitialized) {
            playerViewModel.player.stop()
        }
    }

    override fun onDestroy() {
        hideChannelJob?.cancel()
        if (::playerViewModel.isInitialized) {
            playerViewModel.releasePlayer()
        }
        super.onDestroy()
    }
}
