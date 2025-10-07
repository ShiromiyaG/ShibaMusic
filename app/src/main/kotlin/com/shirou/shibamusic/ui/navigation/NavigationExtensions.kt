package com.shirou.shibamusic.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

/**
 * Extension functions for Navigation
 */

/**
 * Navigate to a destination with default options
 * - Single top (no duplicates)
 * - Pop up to start destination
 * - Save/restore state
 */
fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Navigate and clear back stack
 */
fun NavController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
    }
}

/**
 * Navigate to album detail
 */
fun NavController.navigateToAlbum(albumId: String) {
    navigate(Screen.Album.createRoute(albumId))
}

/**
 * Navigate to artist detail
 */
fun NavController.navigateToArtist(artistId: String) {
    navigate(Screen.Artist.createRoute(artistId))
}

/**
 * Navigate to playlist detail
 */
fun NavController.navigateToPlaylist(playlistId: String) {
    navigate(Screen.Playlist.createRoute(playlistId))
}

/**
 * Navigate to player screen
 */
fun NavController.navigateToPlayer() {
    navigate(Screen.Player.route)
}

/**
 * Navigate to queue screen
 */
fun NavController.navigateToQueue() {
    navigate(Screen.Queue.route)
}

/**
 * Navigate to search screen
 */
fun NavController.navigateToSearch() {
    navigate(Screen.Search.route)
}

/**
 * Navigate to library screen
 */
fun NavController.navigateToLibrary() {
    navigate(Screen.Library.route)
}

/**
 * Navigate to settings
 */
fun NavController.navigateToSettings() {
    navigate(Screen.Settings.route)
}

/**
 * Safe navigation that checks if destination exists
 */
fun NavController.safeNavigate(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    try {
        navigate(route, builder)
    } catch (e: Exception) {
        // Log error or show message
        println("Navigation error: ${e.message}")
    }
}

/**
 * Check if current destination is the given route
 */
fun NavController.isCurrentDestination(route: String): Boolean {
    return currentDestination?.route == route
}
