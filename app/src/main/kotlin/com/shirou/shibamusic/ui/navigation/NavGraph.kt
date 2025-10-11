package com.shirou.shibamusic.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.shirou.shibamusic.ui.LocalSharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.screens.*
import com.shirou.shibamusic.ui.screens.library.*
import com.shirou.shibamusic.ui.player.PlayerScreen
import com.shirou.shibamusic.ui.viewmodel.AlbumDetailViewModel
import com.shirou.shibamusic.ui.viewmodel.ArtistDetailViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaylistDetailViewModel
import com.shirou.shibamusic.ui.viewmodel.PlayerViewModel
import com.shirou.shibamusic.ui.viewmodel.SearchViewModel
import com.shirou.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.ui.viewmodel.PlaylistDetailEvent
import android.util.Log

/**
 * Main navigation graph for the app
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ShibaMusicNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToLibrary = {
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.Album.createRoute(albumId))
                }
            )
        }
        
        // Search Screen
        composable(Screen.Search.route) { backStackEntry ->
            val searchViewModel: SearchViewModel = hiltViewModel(backStackEntry)
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val query by searchViewModel.searchQuery.collectAsState()
            val uiState by searchViewModel.uiState.collectAsState()
            val playbackState by playbackViewModel.playbackState.collectAsState()

            SearchScreen(
                query = query,
                onQueryChange = searchViewModel::updateQuery,
                searchResults = uiState.results,
                isSearching = uiState.isSearching,
                currentSongId = playbackState.nowPlaying?.id,
                isPlaying = playbackState.isPlaying,
                onBackClick = { navController.navigateUp() },
                onSongClick = { song ->
                    playbackViewModel.playSong(song)
                    searchViewModel.playSong(song.id)
                },
                onAlbumClick = { album ->
                    navController.navigate(Screen.Album.createRoute(album.id))
                },
                onAlbumPlay = { album ->
                    playbackViewModel.playAlbum(album.id)
                },
                onArtistClick = { artist ->
                    navController.navigate(Screen.Artist.createRoute(artist.id))
                },
                onArtistPlay = { artist ->
                    playbackViewModel.playArtist(artist.id)
                },
                onClearQuery = searchViewModel::clearQuery,
                errorMessage = uiState.error,
                onDismissError = searchViewModel::clearError
            )
        }
        
        // Library - Main with Tabs
        composable(Screen.Library.route) {
            LibraryScreen(
                navController = navController,
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Screen.Album.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Screen.Artist.createRoute(artistId))
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Screen.Playlist.createRoute(playlistId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Album Detail
        composable(
            route = Screen.Album.route,
            arguments = listOf(
                navArgument(NavArgs.ALBUM_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(NavArgs.ALBUM_ID) ?: return@composable
            val albumViewModel: AlbumDetailViewModel = hiltViewModel(backStackEntry)
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val offlineViewModel: OfflineViewModel = hiltViewModel()
            val uiState by albumViewModel.uiState.collectAsState()
            val playbackState by playbackViewModel.playbackState.collectAsState()

            AlbumScreen(
                albumDetail = uiState.album,
                isLoading = uiState.isLoading,
                currentlyPlayingSongId = playbackState.nowPlaying?.id,
                onNavigateBack = { navController.navigateUp() },
                onPlayClick = {
                    uiState.songs.takeIf { it.isNotEmpty() }?.let { songs ->
                        playbackViewModel.playSongs(songs)
                    }
                },
                onShuffleClick = {
                    uiState.songs.takeIf { it.isNotEmpty() }?.let { songs ->
                        playbackViewModel.playSongs(songs.shuffled())
                    }
                },
                onDownloadClick = {
                    val songs = uiState.songs
                    Log.d("NavGraph", "Album download clicked for albumId: $albumId (songs=${songs.size})")
                    offlineViewModel.downloadAlbum(albumId, songs)
                },
                onSongClick = { song -> playbackViewModel.playSong(song) },
                onSongMenuClick = { /* TODO: Show song actions */ },
                onArtistClick = {
                    uiState.album?.album?.artistId?.let { artistId ->
                        navController.navigate(Screen.Artist.createRoute(artistId))
                    }
                }
            )
        }
        
        // Artist Detail
        composable(
            route = Screen.Artist.route,
            arguments = listOf(
                navArgument(NavArgs.ARTIST_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString(NavArgs.ARTIST_ID) ?: return@composable
            val artistViewModel: ArtistDetailViewModel = hiltViewModel(backStackEntry)
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val offlineViewModel: OfflineViewModel = hiltViewModel()
            val uiState by artistViewModel.uiState.collectAsState()
            val playbackState by playbackViewModel.playbackState.collectAsState()

            when {
                uiState.artist == null && uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.artist == null && uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.error ?: "Unknown error")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { artistViewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.artist != null -> {
                    val artistDetail = requireNotNull(uiState.artist)
                    ArtistScreen(
                        artist = artistDetail,
                        popularSongs = uiState.popularSongs,
                        albums = uiState.albums,
                        currentSongId = playbackState.nowPlaying?.id,
                        isPlaying = playbackState.isPlaying,
                        onBackClick = { navController.navigateUp() },
                        onPlayClick = { playbackViewModel.playArtist(artistId) },
                        onShuffleClick = {
                            if (uiState.songs.isNotEmpty()) {
                                playbackViewModel.playSongs(uiState.songs.shuffled())
                            }
                        },
                        onDownloadClick = {
                            val songs = uiState.songs
                            Log.d("NavGraph", "Artist download clicked for artistId: $artistId (songs=${songs.size})")
                            offlineViewModel.downloadArtist(artistId, songs)
                        },
                        onSongClick = { song -> playbackViewModel.playSong(song) },
                        onAlbumClick = { album ->
                            navController.navigate(Screen.Album.createRoute(album.id))
                        },
                        onSongGoToAlbum = { albumId ->
                            navController.navigate(Screen.Album.createRoute(albumId))
                        },
                        onMenuClick = { /* TODO: Show artist menu */ }
                    )
                }
            }
        }
        
        // Playlist Detail
        composable(
            route = Screen.Playlist.route,
            arguments = listOf(
                navArgument(NavArgs.PLAYLIST_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString(NavArgs.PLAYLIST_ID) ?: return@composable
            val playlistViewModel: PlaylistDetailViewModel = hiltViewModel(backStackEntry)
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val offlineViewModel: OfflineViewModel = hiltViewModel()
            val uiState by playlistViewModel.uiState.collectAsState()
            val playbackState by playbackViewModel.playbackState.collectAsState()

            LaunchedEffect(playlistId) {
                playlistViewModel.events.collect { event ->
                    when (event) {
                        PlaylistDetailEvent.PlaylistDeleted -> navController.navigateUp()
                    }
                }
            }

            when {
                uiState.playlist == null && uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.playlist == null && uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.error ?: "Unknown error")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { playlistViewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.playlist != null -> {
                    val playlist = requireNotNull(uiState.playlist)
                    Box(modifier = Modifier.fillMaxSize()) {
                        PlaylistDetailScreen(
                            playlist = playlist,
                            songs = uiState.songs,
                            currentSongId = playbackState.nowPlaying?.id,
                            isPlaying = playbackState.isPlaying,
                            onBackClick = { navController.navigateUp() },
                            onPlayClick = {
                                if (uiState.songs.isNotEmpty()) {
                                    playbackViewModel.playSongs(uiState.songs)
                                }
                            },
                            onShuffleClick = {
                                if (uiState.songs.isNotEmpty()) {
                                    playbackViewModel.playSongs(uiState.songs.shuffled())
                                }
                            },
                            onSongClick = { song -> playbackViewModel.playSong(song) },
                            onEditPlaylist = { /* TODO: Navigate to edit playlist */ },
                            onDeletePlaylist = {
                                if (!uiState.isProcessing) {
                                    playlistViewModel.deletePlaylist()
                                }
                            },
                            onAddSongs = { /* TODO: Navigate to add songs */ },
                            onSongGoToAlbum = { albumId ->
                                navController.navigate(Screen.Album.createRoute(albumId))
                            },
                            onSongGoToArtist = { artistId ->
                                navController.navigate(Screen.Artist.createRoute(artistId))
                            },
                            onDownloadClick = {
                                offlineViewModel.downloadPlaylist(playlist.id, uiState.songs)
                            }
                        )

                        if (uiState.isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
        
        composable(
            route = Screen.Player.route,
            enterTransition = {
                fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            val sharedTransitionScope = LocalSharedTransitionScope.current
            if (sharedTransitionScope != null) {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onShowLyrics = {
                        navController.navigate(Screen.Lyrics.route)
                    },
                    onShowQueue = {
                        navController.navigate(Screen.Queue.route)
                    },
                    onMoreClick = { /* TODO: Show more options */ },
                    onNavigateToAlbum = { albumId ->
                        navController.navigate(Screen.Album.createRoute(albumId))
                    },
                    onNavigateToArtist = { artistId ->
                        navController.navigate(Screen.Artist.createRoute(artistId))
                    },
                    animatedVisibilityScope = this@composable,
                    sharedTransitionScope = sharedTransitionScope
                )
            }
        }
        
        // Queue Screen
        composable(Screen.Queue.route) {
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val playerState by playerViewModel.playerState.collectAsState()

            QueueScreen(
                queue = playerState.queue,
                currentIndex = playerState.currentIndex,
                isPlaying = playerState.isPlaying,
                onBackClick = { navController.navigateUp() },
                onSongClick = { index -> playerViewModel.seekToSong(index) },
                onRemoveSong = { index -> playerViewModel.removeFromQueue(index) },
                onClearQueue = { playerViewModel.clearQueue() },
                onSaveQueue = { /* TODO: Save queue as playlist */ }
            )
        }
        
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to home after successful login
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            com.shirou.shibamusic.ui.screens.settings.SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
    }
}
