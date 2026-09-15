package com.example.genritv.ui

/**
 * Presentation state shared by the TV shell and individual screens.
 * Keeping navigation/player state out of the Activity makes the UI easier to test and evolve.
 */
data class AppUiState(
    val route: AppRoute = AppRoute.Home,
    val mode: AppMode = AppMode.CHANNELS,
    val selectedChannelIndex: Int = 0,
    val channelName: String = "",
    val channelLogo: String? = null,
    val showChannelOverlay: Boolean = false,
    val resizeMode: Int = 0,
)

enum class AppRoute {
    Home,
    Player,
}

enum class AppMode {
    CHANNELS,
    VOD,
    SERIES,
}
