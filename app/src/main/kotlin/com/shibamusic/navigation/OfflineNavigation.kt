package com.shibamusic.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shibamusic.ui.offline.OfflineScreen

/**
 * Rota para a tela offline
 */
const val OFFLINE_ROUTE = "offline"

/**
 * Adiciona a navegação offline ao grafo de navegação
 */
fun NavGraphBuilder.offlineScreen() {
    composable(route = OFFLINE_ROUTE) {
        OfflineScreen()
    }
}

/**
 * Destinos de navegação offline
 */
sealed class OfflineDestinations(val route: String) {
    object Offline : OfflineDestinations(OFFLINE_ROUTE)
}