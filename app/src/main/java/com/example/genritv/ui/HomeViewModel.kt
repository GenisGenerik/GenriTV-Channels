package com.example.genritv.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genritv.data.ChannelClassifier
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
        _channels,
        _movies,
        _series,
        _searchQuery,
        _selectedCategory
    ) { channels, movies, series, query, category ->
        val normalizedQuery = query.trim()

        val filteredChannels = when (category) {
            HomeCategory.TV_NASIONAL -> channels.filter { channel ->
                ChannelClassifier.isNational(channel) && matches(channel.nama, normalizedQuery)
            }
            HomeCategory.TV_REGIONAL -> channels.filter { channel ->
                ChannelClassifier.isRegional(channel) && matches(channel.nama, normalizedQuery)
            }
            HomeCategory.HOME -> channels.filter { matches(it.nama, normalizedQuery) }
            else -> emptyList()
        }

        val filteredMovies = if (category == HomeCategory.HOME || category == HomeCategory.MOVIES) {
            movies.filter { matches(it.title, normalizedQuery) }
        } else {
            emptyList()
        }

        val filteredSeries = if (category == HomeCategory.HOME || category == HomeCategory.SERIES) {
            series.filter { matches(it.title, normalizedQuery) }
        } else {
            emptyList()
        }

        HomeState(
            channels = filteredChannels,
            movies = filteredMovies,
            seriesList = filteredSeries,
            selectedCategory = category,
            isSearching = normalizedQuery.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeState()
    )

    fun loadData(context: Context) {
        viewModelScope.launch {
            runCatching {
                Triple(
                    UnifiedChannelRepository.loadChannels(context.applicationContext),
                    VodRepository.getMovies(),
                    SeriesRepository.getSeries()
                )
            }.onSuccess { (channels, movies, series) ->
                _channels.value = channels
                _movies.value = movies
                _series.value = series
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query.take(MAX_SEARCH_LENGTH)
    }

    fun onCategorySelected(category: HomeCategory) {
        _selectedCategory.value = category
    }

    private fun matches(value: String, query: String): Boolean =
        query.isEmpty() || value.contains(query, ignoreCase = true)

    companion object {
        private const val MAX_SEARCH_LENGTH = 80
    }
}

data class HomeState(
    val channels: List<TvChannel> = emptyList(),
    val movies: List<VodMovie> = emptyList(),
    val seriesList: List<Series> = emptyList(),
    val selectedCategory: HomeCategory = HomeCategory.HOME,
    val isSearching: Boolean = false
)
