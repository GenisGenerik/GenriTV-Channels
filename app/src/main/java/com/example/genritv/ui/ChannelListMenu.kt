package com.example.genritv.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
// ...

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.genritv.model.TvChannel
import java.util.Locale

@Composable
fun ChannelListMenu(
    channels: List<TvChannel>,
    selectedChannelIndex: Int,
    currentChannelIndex: Int,
    onChannelClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll otomatis ke channel yang sedang dipilih saat menu dibuka
    // atau saat navigasi (selectedChannelIndex berubah)
    LaunchedEffect(selectedChannelIndex, channels.size) {
        if (channels.isNotEmpty()) {
            val scrollIndex = selectedChannelIndex.coerceIn(0, channels.lastIndex)
            listState.animateScrollToItem(scrollIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(
                Color.Black.copy(alpha = 0.85f)
            )
            .padding(16.dp)
    ) {
        itemsIndexed(channels) { index, channel ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onChannelClick(index) }
                    .background(
                        color =
                        if (index == selectedChannelIndex)
                            Color.DarkGray
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.nama,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                val prefix = buildString {
                    if (index == selectedChannelIndex) {
                        append("▶ ")
                    }

                    if (index == currentChannelIndex) {
                        append("🔊 ")
                    }
                }
                
                Text(
                    text = "$prefix${channel.nama}",
                    color =
                    when {
                        index == currentChannelIndex ->
                            Color.Green

                        index == selectedChannelIndex ->
                            Color.Yellow

                        else ->
                            Color.White
                    },
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }
        }
    }
}
