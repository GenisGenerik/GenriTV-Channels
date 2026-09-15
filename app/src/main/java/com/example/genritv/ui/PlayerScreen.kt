package com.example.genritv.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.example.genritv.MainActivity
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    player: ExoPlayer,
    channelName: String,
    showChannelName: Boolean,
    channelLogo: String?,
    isLoading: Boolean,
    currentTime: String,
    showError: Boolean,
    currentMode: MainActivity.AppMode,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    resizeMode: Int,
    onRetry: () -> Unit
){
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    this.resizeMode = resizeMode
                    useController = false
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            }
        )

        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        )

        AnimatedVisibility(
            visible = showChannelName,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    AsyncImage(
                        model = channelLogo,
                        contentDescription = channelName,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = channelName,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentMode != MainActivity.AppMode.CHANNELS) {
                            Text(
                                text = if (currentMode == MainActivity.AppMode.VOD) "Film" else "Series",
                                color = Color.Yellow,
                                fontSize = 18.sp
                            )
                        }
                        
                        val ratioText = when(resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Rasio: FIT"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Rasio: STRETCH"
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Rasio: ZOOM"
                            else -> "Rasio: Default"
                        }
                        Text(
                            text = ratioText,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (currentMode != MainActivity.AppMode.CHANNELS) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                if (currentMode != MainActivity.AppMode.CHANNELS && duration > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { position.toFloat() / duration.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color.Yellow,
                            trackColor = Color.Gray.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(position), color = Color.White, fontSize = 16.sp)
                            Text(formatTime(duration), color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(text = "Memuat...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (showError) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Text(text = "⚠ Channel Tidak Tersedia\n\nTekan OK untuk mencoba lagi", color = Color.White, fontSize = 24.sp)
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
