package com.example.genritv.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genritv.data.SeriesRepository
import com.example.genritv.data.UnifiedChannelRepository
import com.example.genritv.data.VodRepository
import com.example.genritv.model.Series
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeCategory {
    HOME, TV_NASIONAL, TV_REGIONAL, MOVIES, SERIES
}

class HomeViewModel : ViewModel() {
    private val _channels = MutableStateFlow<List<TvChannel>>(emptyList())
    private val _movies = MutableStateFlow<List<VodMovie>>(emptyList())
    private val _series = MutableStateFlow<List<Series>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(HomeCategory.HOME)

    val searchQuery: StateFlow<String> = _searchQuery
    val selectedCategory: StateFlow<HomeCategory> = _selectedCategory

    val homeState: StateFlow<HomeState> = combine(
        _channels, _movies, _series, _searchQuery, _selectedCategory
    ) { channels, movies, series, query, category ->
        val filteredChannels = when (category) {
            HomeCategory.TV_NASIONAL -> channels.filter { channel ->
                isNationalChannel(channel) && channel.nama.contains(query, ignoreCase = true)
            }
            HomeCategory.TV_REGIONAL -> channels.filter { channel ->
                isRegionalChannel(channel) && channel.nama.contains(query, ignoreCase = true)
            }
            HomeCategory.HOME -> channels.filter { it.nama.contains(query, ignoreCase = true) }
            else -> emptyList()
        }

        val filteredMovies = if (category == HomeCategory.HOME || category == HomeCategory.MOVIES) {
            movies.filter { it.title.contains(query, ignoreCase = true) }
        } else emptyList()

        val filteredSeries = if (category == HomeCategory.HOME || category == HomeCategory.SERIES) {
            series.filter { it.title.contains(query, ignoreCase = true) }
        } else emptyList()

        HomeState(
            channels = filteredChannels,
            movies = filteredMovies,
            seriesList = filteredSeries,
            selectedCategory = category,
            isSearching = query.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun loadData(context: Context) {
        viewModelScope.launch {
            val loadedChannels = UnifiedChannelRepository.loadChannels(context)
            _channels.value = loadedChannels
            _movies.value = VodRepository.getMovies()
            _series.value = SeriesRepository.getSeries()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: HomeCategory) {
        _selectedCategory.value = category
    }

    private fun isNationalChannel(channel: TvChannel): Boolean {
        val group = channel.grup.orEmpty()
        val name = channel.nama
        val nationalKeywords = listOf(
            "nasional", "indonesia", "rcti", "sctv", "indosiar", "antv",
            "trans", "tvone", "metro", "kompas", "mnc", "gtv", "inews",
            "tvri", "rtv", "net", "garuda", "moji", "daai"
        )
        return nationalKeywords.any { keyword ->
            group.contains(keyword, ignoreCase = true) || name.contains(keyword, ignoreCase = true)
        }
    }

    private fun isRegionalChannel(channel: TvChannel): Boolean {
        val group = channel.grup.orEmpty()
        return group.contains("regional", ignoreCase = true) ||
            group.contains("daerah", ignoreCase = true)
    }
}

data class HomeState(
    val channels: List<TvChannel> = emptyList(),
    val movies: List<VodMovie> = emptyList(),
    val seriesList: List<Series> = emptyList(),
    val selectedCategory: HomeCategory = HomeCategory.HOME,
    val isSearching: Boolean = false
)
