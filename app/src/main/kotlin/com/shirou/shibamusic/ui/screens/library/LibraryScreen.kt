package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shirou.shibamusic.ui.component.NavigationTitle
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel

/**
 * Library Screen with Tabs (Songs, Albums, Artists, Playlists)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Lazy load ViewModels apenas quando necessário
    val songsViewModel: com.shirou.shibamusic.ui.viewmodel.LibrarySongsViewModel? = 
        if (selectedTab == LibraryTab.SONGS) hiltViewModel() else null
    val albumsViewModel: com.shirou.shibamusic.ui.viewmodel.LibraryAlbumsViewModel? = 
        if (selectedTab == LibraryTab.ALBUMS) hiltViewModel() else null
    val artistsViewModel: com.shirou.shibamusic.ui.viewmodel.LibraryArtistsViewModel? = 
        if (selectedTab == LibraryTab.ARTISTS) hiltViewModel() else null
    val playlistsViewModel: com.shirou.shibamusic.ui.viewmodel.LibraryPlaylistsViewModel? = 
        if (selectedTab == LibraryTab.PLAYLISTS) hiltViewModel() else null
    
    // Column com TopBar e Tabs integrados
    Column(modifier = modifier.fillMaxSize()) {
        // TopBar
        TopAppBar(
            title = {
                NavigationTitle(title = "Library")
            },
            actions = {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        when (selectedTab) {
                            LibraryTab.SONGS -> songsViewModel?.let { vm ->
                                val songsState by vm.uiState.collectAsState()
                                com.shirou.shibamusic.ui.viewmodel.SongSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            vm.changeSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (songsState.sortOption == option) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                            LibraryTab.ALBUMS -> albumsViewModel?.let { vm ->
                                val albumsState by vm.uiState.collectAsState()
                                com.shirou.shibamusic.ui.viewmodel.AlbumSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            vm.changeSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (albumsState.sortOption == option) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                            LibraryTab.ARTISTS -> artistsViewModel?.let { vm ->
                                val artistsState by vm.uiState.collectAsState()
                                com.shirou.shibamusic.ui.viewmodel.ArtistSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            vm.changeSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (artistsState.sortOption == option) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                            LibraryTab.PLAYLISTS -> playlistsViewModel?.let { vm ->
                                val playlistsState by vm.uiState.collectAsState()
                                com.shirou.shibamusic.ui.viewmodel.PlaylistSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            vm.changeSortOption(option)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (playlistsState.sortOption == option) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = WindowInsets(0.dp)
        )
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LibraryTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    }
                )
            }
        }
        
        // Conteúdo com lazy loading
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                LibraryTab.SONGS -> songsViewModel?.let { vm ->
                    LibrarySongsScreen(
                        viewModel = vm,
                        currentlyPlayingSongId = playbackState.nowPlaying?.id,
                        onSongClick = { song ->
                            playbackViewModel.playSong(song)
                        },
                        onSongMenuClick = { song ->
                            // TODO: Show menu with options (Add to Queue, Play Next, Add to Playlist, etc.)
                        },
                        onShuffleAllClick = {
                            playbackViewModel.shuffleAll()
                        }
                    )
                }
                LibraryTab.ALBUMS -> albumsViewModel?.let { vm ->
                    LibraryAlbumsScreen(
                        viewModel = vm,
                        onAlbumClick = { album ->
                            onNavigateToAlbum(album.id)
                        },
                        onAlbumPlay = { album ->
                            playbackViewModel.playAlbum(album.id)
                        }
                    )
                }
                LibraryTab.ARTISTS -> artistsViewModel?.let { vm ->
                    LibraryArtistsScreen(
                        onArtistClick = { artist ->
                            onNavigateToArtist(artist.id)
                        },
                        onArtistPlay = { artist ->
                            playbackViewModel.playArtist(artist.id)
                        }
                    )
                }
                LibraryTab.PLAYLISTS -> playlistsViewModel?.let { vm ->
                    LibraryPlaylistsScreen(
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylist(playlist.id)
                        },
                        onCreatePlaylist = { /* TODO: Show create dialog */ },
                        onPlaylistPlay = { playlist ->
                            playbackViewModel.playPlaylist(playlist.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Library tabs enum
 */
enum class LibraryTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    SONGS("Songs", Icons.Rounded.MusicNote),
    ALBUMS("Albums", Icons.Rounded.Album),
    ARTISTS("Artists", Icons.Rounded.Person),
    PLAYLISTS("Playlists", Icons.Rounded.QueueMusic)
}
