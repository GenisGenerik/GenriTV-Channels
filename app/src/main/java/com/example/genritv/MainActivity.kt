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

import com.example.genritv.data.M3uRepository
import com.example.genritv.model.Series
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
import com.example.genritv.ui.PlayerScreen
@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    companion object {
        private const val PREFS_NAME = "genri_tv_prefs"
        private const val KEY_LAST_CHANNEL = "last_channel"
    }
    private lateinit var player: ExoPlayer
    private var currentRoute by mutableStateOf("home")
    private var channels by mutableStateOf<List<TvChannel>>(emptyList())
    private var currentChannelName by mutableStateOf("")
    private var currentChannelLogo by mutableStateOf<String?>(null)
    private var currentChannelIndex = 0
    private var showChannelName by mutableStateOf(false)
    private var hideChannelJob: Job? = null

    // New navigation states
    enum class AppMode { CHANNELS, VOD, SERIES }
    private var currentMode by mutableStateOf(AppMode.CHANNELS)

    private var playerPosition by mutableStateOf(0L)
    private var playerDuration by mutableStateOf(0L)
    private var isPlayerPlaying by mutableStateOf(false)

    private var resizeMode by mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private var retryCount = 0
    private val MAX_RETRIES = 3

    private var isLoading by mutableStateOf(false)
    private var currentTime by mutableStateOf("")
    private var showError by mutableStateOf(false)
    private fun startClock() {

        lifecycleScope.launch {

            while (true) {

                currentTime = java.text.SimpleDateFormat(
                    "HH:mm",
                    java.util.Locale.getDefault()
                ).format(
                    java.util.Date()
                )

                delay(1000)
            }
        }
    }
    private fun playChannel(index: Int, urlIndex: Int = 0) {
        if (channels.isEmpty()) return
        
        val lastIdx = channels.lastIndex
        currentChannelIndex = if (lastIdx >= 0) index.coerceIn(0, lastIdx) else 0
        saveCurrentChannel()
        
        if (currentChannelIndex !in channels.indices) return
        
        val channel = channels[currentChannelIndex]
        currentChannelName = channel.nama
        currentChannelLogo = channel.logo
        
        showChannelName = true

        hideChannelJob?.cancel()

        hideChannelJob = MainScope().launch {

            delay(3000)

            showChannelName = false
        }
        
        val streamUrl = channel.urls.getOrNull(urlIndex) ?: channel.url
        Log.d("GENRI_TV", "Play: ${channel.nama} (URL Index: $urlIndex) -> $streamUrl")

        val mediaItem = MediaItem.fromUri(streamUrl)
        showError = false
        // retryCount should be managed by the caller if it's a manual play vs auto retry
        player.setMediaItem(mediaItem)

        player.prepare()

        player.playWhenReady = true
    }
    private fun playVod(movie: VodMovie) {
        currentMode = AppMode.VOD
        currentChannelName = movie.title
        currentChannelLogo = movie.logo
        
        showChannelName = true
        hideChannelJob?.cancel()
        hideChannelJob = MainScope().launch {
            delay(3000)
            showChannelName = false
        }

        val mediaItem = MediaItem.fromUri(movie.url)
        showError = false
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun playSeries(series: Series) {
        currentMode = AppMode.SERIES
        currentChannelName = series.title
        currentChannelLogo = series.logo
        
        showChannelName = true
        hideChannelJob?.cancel()
        hideChannelJob = MainScope().launch {
            delay(3000)
            showChannelName = false
        }

        // Mock URL for series
        val mediaItem = MediaItem.fromUri("https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4")
        showError = false
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun saveCurrentChannel() {
        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_LAST_CHANNEL,
                currentChannelIndex
            )
            .apply()
    }
    private fun loadLastChannel(): Int {
        if (channels.isEmpty()) return 0

        val savedIndex = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).getInt(
            KEY_LAST_CHANNEL,
            0
        )

        return savedIndex.coerceIn(
            0,
            channels.lastIndex
        )
    }
    private fun nextChannel() {

        val nextIndex =
            if (currentChannelIndex == channels.lastIndex) {
                0
            } else {
                currentChannelIndex + 1
            }

        playChannel(nextIndex)
    }
    private fun previousChannel() {

        val previousIndex =
            if (currentChannelIndex == 0) {
                channels.lastIndex
            } else {
                currentChannelIndex - 1
            }

        playChannel(previousIndex)
    }
    private fun reloadPlaylist() {

        lifecycleScope.launch {

            try {

                channels =
                    M3uRepository.loadChannels(this@MainActivity)
                currentChannelName = "Playlist Reloaded"
                showChannelName = true
                Log.d(
                    "GENRI_TV",
                    "Playlist berhasil direload"
                )
                if (currentChannelIndex > channels.lastIndex) {

                    currentChannelIndex = 0
                }

            } catch (e: Exception) {

                Log.e(
                    "GENRI_TV",
                    "Reload gagal",
                    e
                )
            }
        }
    }
    private var lastKeyDownTime = 0L
    private val KEY_DEBOUNCE_MS = 250L

    override fun onKeyDown(
        keyCode: Int,
        event: android.view.KeyEvent?
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastKeyDownTime < KEY_DEBOUNCE_MS) {
            return true // Ignore rapid clicks
        }
        lastKeyDownTime = currentTime

        // Hanya izinkan navigasi channel/menu jika di layar player
        if (currentRoute != "player" && keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU) {
            return super.onKeyDown(keyCode, event)
        }

        if (channels.isEmpty() && keyCode != KeyEvent.KEYCODE_MENU) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {

            android.view.KeyEvent.KEYCODE_DPAD_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (currentMode == AppMode.CHANNELS) {
                    previousChannel()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN
                -> {
                if (currentMode == AppMode.CHANNELS) {
                    nextChannel()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (currentMode != AppMode.CHANNELS) {
                    player.seekBack()
                    showChannelName = true
                    resetHideJob()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (currentMode != AppMode.CHANNELS) {
                    player.seekForward()
                    showChannelName = true
                    resetHideJob()
                }
                return true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_UP,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {

                if (showError) {
                    if (currentMode == AppMode.CHANNELS) {
                        playChannel(currentChannelIndex)
                    }
                    return true
                }
                
                if (currentMode != AppMode.CHANNELS) {
                    if (player.isPlaying) player.pause() else player.play()
                }

                showChannelName = true
                resetHideJob()

                return true
            }
            KeyEvent.KEYCODE_PROG_GREEN,
            KeyEvent.KEYCODE_R -> {
                cycleResizeMode()
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                reloadPlaylist()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }
    private fun cycleResizeMode() {
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        showChannelName = true
        resetHideJob()
    }

    private fun resetHideJob() {
        hideChannelJob?.cancel()
        hideChannelJob = MainScope().launch {
            delay(if (currentMode == AppMode.CHANNELS) 3000 else 7000)
            showChannelName = false
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Konfigurasi Player Berkualitas Tinggi (Production Ready)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // Min buffer 30s
                60000, // Max buffer 60s
                2500,  // Buffer to start playback 2.5s
                5000   // Buffer to resume playback 5s
            )
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("GenriTV/1.0 (Linux; Android TV)")
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    Log.e("GENRI_TV", "Player Error: ${error.message}", error)
                    
                    if (currentMode == AppMode.CHANNELS) {
                        val channel = channels.getOrNull(currentChannelIndex)
                        val totalUrls = channel?.urls?.size ?: 1
                        
                        // Try all URLs, then retry up to MAX_RETRIES if only 1 URL
                        val canRetry = if (totalUrls > 1) {
                            retryCount < totalUrls - 1 || retryCount < MAX_RETRIES
                        } else {
                            retryCount < MAX_RETRIES
                        }

                        if (canRetry) {
                            retryCount++
                            lifecycleScope.launch {
                                val nextUrlIdx = if (totalUrls > 1) retryCount % totalUrls else 0
                                Log.d("GENRI_TV", "Attempting retry $retryCount (URL Index: $nextUrlIdx)...")
                                delay(2000)
                                playChannel(currentChannelIndex, nextUrlIdx)
                            }
                        } else {
                            showError = true
                            retryCount = 0
                        }
                    } else {
                        // VOD/Series retry
                        if (retryCount < MAX_RETRIES) {
                            retryCount++
                            lifecycleScope.launch {
                                Log.d("GENRI_TV", "Attempting VOD retry $retryCount...")
                                delay(2000)
                                player.prepare()
                                player.play()
                            }
                        } else {
                            showError = true
                            retryCount = 0
                        }
                    }
                }
            }
        )
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

        setContent {
            val navController = rememberNavController()
            
            // Handle navigasi tombol Back sesuai standar TV
            BackHandler(enabled = true) {
                if (currentRoute == "player") {
                    navController.popBackStack("home", inclusive = false)
                } else {
                    player.stop()
                    player.release()
                    finishAndRemoveTask()
                }
            }

            // Auto-Navigate ke player saat data channel siap dan auto-play dijalankan
            LaunchedEffect(channels) {
                if (channels.isNotEmpty() && currentRoute == "home") {
                    val lastChannel = loadLastChannel()
                    currentMode = AppMode.CHANNELS
                    retryCount = 0
                    playChannel(lastChannel)
                    navController.navigate("player")
                }
            }

            // Sync currentRoute & player stop logic
            navController.addOnDestinationChangedListener { _, destination, _ ->
                currentRoute = destination.route ?: "home"
                if (currentRoute != "player") {
                    player.stop()
                }
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        onNavigateToLiveTv = { channel ->
                            currentMode = AppMode.CHANNELS
                            val index = channels.indexOf(channel)
                            if (index != -1) {
                                retryCount = 0 // Reset retry count for manual play
                                playChannel(index)
                            }
                            navController.navigate("player")
                        },
                        onNavigateToMovies = { movie ->
                            playVod(movie)
                            navController.navigate("player")
                        },
                        onNavigateToSeries = { series ->
                            playSeries(series)
                            navController.navigate("player")
                        },
                        onNavigateToSettings = { 
                            Log.d("GENRI_TV", "Settings Clicked")
                        }
                    )
                }
                composable("player") {
                    PlayerScreen(
                        player = player,
                        channelName = currentChannelName,
                        channelLogo = currentChannelLogo,
                        showChannelName = showChannelName,
                        isLoading = isLoading,
                        currentTime = currentTime,
                        showError = showError,
                        currentMode = currentMode,
                        isPlaying = isPlayerPlaying,
                        position = playerPosition,
                        duration = playerDuration,
                        resizeMode = resizeMode,
                        onRetry = { playChannel(currentChannelIndex) }
                    )
                }
            }
        }

        lifecycleScope.launch {
            try {
                val loadedChannels = M3uRepository.loadChannels(this@MainActivity)
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
        // Melanjutkan tayangan otomatis saat kembali dari background/home remote
        if (currentRoute == "player" && !player.isPlaying) {
            player.play()
        }
    }

    override fun onPause() {
        super.onPause()
        // Hentikan suara saat aplikasi tidak terlihat (misal tekan Home)
        player.pause()
    }

    override fun onStop() {
        super.onStop()
        // Pastikan player berhenti total
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

