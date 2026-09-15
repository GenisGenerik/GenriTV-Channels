package com.example.genritv.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import coil.compose.AsyncImage
import com.example.genritv.model.Series
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
import com.example.genritv.ui.theme.GenriTVTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLiveTv: (TvChannel) -> Unit,
    onNavigateToMovies: (VodMovie) -> Unit,
    onNavigateToSeries: (Series) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.homeState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    NavigationDrawer(
        drawerContent = { drawerValue ->
            val isExpanded = drawerValue == DrawerValue.Open
            val drawerWidth by animateDpAsState(
                targetValue = if (isExpanded) 280.dp else 80.dp,
                label = "drawerWidth"
            )
            
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .background(Color(0xFF0F0F0F))
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Yellow, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                
                Spacer(modifier = Modifier.height(48.dp))

                DrawerItem("Home", Icons.Default.Home, selectedCategory == HomeCategory.HOME, isExpanded) {
                    viewModel.onCategorySelected(HomeCategory.HOME)
                }
                DrawerItem("TV Nasional", Icons.Default.LiveTv, selectedCategory == HomeCategory.TV_NASIONAL, isExpanded) {
                    viewModel.onCategorySelected(HomeCategory.TV_NASIONAL)
                }
                DrawerItem("TV Regional", Icons.Default.Map, selectedCategory == HomeCategory.TV_REGIONAL, isExpanded) {
                    viewModel.onCategorySelected(HomeCategory.TV_REGIONAL)
                }
                DrawerItem("Film", Icons.Default.Movie, selectedCategory == HomeCategory.MOVIES, isExpanded) {
                    viewModel.onCategorySelected(HomeCategory.MOVIES)
                }
                DrawerItem("Series", Icons.Default.Tv, selectedCategory == HomeCategory.SERIES, isExpanded) {
                    viewModel.onCategorySelected(HomeCategory.SERIES)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                DrawerItem("Settings", Icons.Default.Settings, false, isExpanded) {
                    onNavigateToSettings()
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (selectedCategory) {
                HomeCategory.HOME -> HomeOverview(
                    state, searchQuery,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    onNavigateToLiveTv, onNavigateToMovies, onNavigateToSeries
                )
                else -> CategoryGridView(
                    state, searchQuery,
                    onSearchChange = { viewModel.onSearchQueryChange(it) },
                    onNavigateToLiveTv, onNavigateToMovies, onNavigateToSeries,
                    onBackToHome = { viewModel.onCategorySelected(HomeCategory.HOME) }
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .background(
                color = if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Yellow else if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
            
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = if (isSelected) Color.Yellow else Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeOverview(
    state: HomeState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateToLiveTv: (TvChannel) -> Unit,
    onNavigateToMovies: (VodMovie) -> Unit,
    onNavigateToSeries: (Series) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        item {
            HeroSection(searchQuery, onSearchQueryChange = onSearchChange)
        }

        item {
            SectionHeader("STASIUN TV TERPOPULER")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(state.channels.take(12)) { channel ->
                    PosterCard(
                        title = channel.nama,
                        posterUrl = channel.logo,
                        onClick = { onNavigateToLiveTv(channel) },
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }

        item {
            SectionHeader("FILM UNGGULAN")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(state.movies.take(12)) { movie ->
                    PosterCard(
                        title = movie.title,
                        posterUrl = movie.logo,
                        onClick = { onNavigateToMovies(movie) },
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }

        item {
            SectionHeader("SERIES PILIHAN")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(state.seriesList.take(12)) { series ->
                    PosterCard(
                        title = series.title,
                        posterUrl = series.logo,
                        onClick = { onNavigateToSeries(series) },
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryGridView(
    state: HomeState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateToLiveTv: (TvChannel) -> Unit,
    onNavigateToMovies: (VodMovie) -> Unit,
    onNavigateToSeries: (Series) -> Unit,
    onBackToHome: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBackToHome,
                    colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kembali")
                }
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = state.selectedCategory.name.replace("_", " "),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            SearchBox(searchQuery, onSearchChange, width = 350.dp)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (state.selectedCategory) {
                HomeCategory.TV_NASIONAL, HomeCategory.TV_REGIONAL -> {
                    items(state.channels) { channel ->
                        PosterCard(channel.nama, channel.logo, { onNavigateToLiveTv(channel) }, Modifier.fillMaxWidth())
                    }
                }
                HomeCategory.MOVIES -> {
                    items(state.movies) { movie ->
                        PosterCard(movie.title, movie.logo, { onNavigateToMovies(movie) }, Modifier.fillMaxWidth())
                    }
                }
                HomeCategory.SERIES -> {
                    items(state.seriesList) { series ->
                        PosterCard(series.title, series.logo, { onNavigateToSeries(series) }, Modifier.fillMaxWidth())
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color.Black),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GENRI TV",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Nikmati tayangan TV, Film, dan Series terbaik dalam satu aplikasi.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.width(600.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SearchBox(searchQuery, onSearchQueryChange)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchBox(query: String, onQueryChange: (String) -> Unit, width: Dp = 450.dp) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = { focusRequester.requestFocus() },
        modifier = Modifier
            .width(width)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color.Yellow))
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) Color.White else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(
                        text = "Cari acara favorit Anda...",
                        color = if (isFocused) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontSize = 18.sp
                    )
                    inner()
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 48.dp, top = 40.dp, bottom = 16.dp),
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(title: String, posterUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(2f/3f),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.Yellow)))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!posterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 200f
                        )
                    )
            )
            
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun HomeScreenPreview() {
    GenriTVTheme {
        HomeScreen(
            onNavigateToLiveTv = { _ -> },
            onNavigateToMovies = { _ -> },
            onNavigateToSeries = { _ -> },
            onNavigateToSettings = {}
        )
    }
}
