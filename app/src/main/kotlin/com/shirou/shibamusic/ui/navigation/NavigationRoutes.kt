package com.shirou.shibamusic.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search

/**
 * Navigation routes for the app
 * Sealed class to ensure type safety
 */
sealed class Screen(val route: String) {
    // Main Screens
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    
    // Library Tabs
    object LibrarySongs : Screen("library/songs")
    object LibraryAlbums : Screen("library/albums")
    object LibraryArtists : Screen("library/artists")
    object LibraryPlaylists : Screen("library/playlists")
    
    // Detail Screens
    object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    
    object Artist : Screen("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }
    
    object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    
    // Player Screens
    object Player : Screen("player")
    object Queue : Screen("queue")
    object Lyrics : Screen("lyrics")
    
    // Authentication
    object Login : Screen("login")
    
    // Settings
    object Settings : Screen("settings")
    object About : Screen("settings/about")
    object Appearance : Screen("settings/appearance")
    
    // Other
    object Downloads : Screen("downloads")
    object Favorites : Screen("favorites")
}

/**
 * Navigation arguments keys
 */
object NavArgs {
    const val ALBUM_ID = "albumId"
    const val ARTIST_ID = "artistId"
    const val PLAYLIST_ID = "playlistId"
    const val SONG_ID = "songId"
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavItem(
        screen = Screen.Home,
        title = "Home",
        icon = androidx.compose.material.icons.Icons.Rounded.Home
    )
    
    object Search : BottomNavItem(
        screen = Screen.Search,
        title = "Search",
        icon = androidx.compose.material.icons.Icons.Rounded.Search
    )
    
    object Library : BottomNavItem(
        screen = Screen.Library,
        title = "Library",
        icon = androidx.compose.material.icons.Icons.Rounded.LibraryMusic
    )
}

/**
 * Get all bottom navigation items
 */
val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Search,
    BottomNavItem.Library
)
