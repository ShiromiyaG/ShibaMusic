package com.shirou.shibamusic.ui.screens.library

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shirou.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.ui.component.NavigationTitle
import com.shirou.shibamusic.ui.component.SongBottomSheet
import com.shirou.shibamusic.ui.component.AlbumBottomSheet
import com.shirou.shibamusic.ui.component.PlaylistBottomSheet
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.data.model.AudioQuality

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
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    offlineViewModel: OfflineViewModel = hiltViewModel()
) {
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var showSortMenu by remember { mutableStateOf(false) }
    val offlineTracks by offlineViewModel.offlineTracks.collectAsStateWithLifecycle()
    val activeDownloads by offlineViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadedSongIds = remember(offlineTracks) { offlineTracks.map { it.id }.toSet() }
    val activeDownloadMap = remember(activeDownloads) { activeDownloads.associateBy { it.trackId } }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    var showSongBottomSheet by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<AlbumItem?>(null) }
    var showAlbumBottomSheet by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistItem?>(null) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
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
                                val songsState by vm.uiState.collectAsStateWithLifecycle()
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
                                val albumsState by vm.uiState.collectAsStateWithLifecycle()
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
                                val artistsState by vm.uiState.collectAsStateWithLifecycle()
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
                                val playlistsState by vm.uiState.collectAsStateWithLifecycle()
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
                            selectedSong = song
                            showSongBottomSheet = true
                        },
                        onShuffleAllClick = {
                            playbackViewModel.shuffleAll()
                        },
                        downloadedSongIds = downloadedSongIds,
                        activeDownloads = activeDownloadMap
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
                        },
                        onAlbumMenuClick = { album ->
                            selectedAlbum = album
                            showAlbumBottomSheet = true
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
                        },
                        onPlaylistMenuClick = { playlist ->
                            selectedPlaylist = playlist
                            showPlaylistBottomSheet = true
                        }
                    )
                }
            }
        }

        if (showSongBottomSheet && selectedSong != null) {
            val song = selectedSong!!
            val isDownloaded = downloadedSongIds.contains(song.id)
            val downloadInfo = activeDownloadMap[song.id]
            val isDownloading = downloadInfo != null

            val selectedQuality = Preferences.getOfflineDownloadQuality()
            val downloadAction: (() -> Unit)? =
                if (!isDownloaded && !isDownloading) {
                    {
                        offlineViewModel.downloadTrack(
                            trackId = song.id,
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName ?: "Singles",
                            duration = song.duration,
                            coverArtUrl = song.getPlayerArtworkUrl(),
                            quality = selectedQuality
                        )
                    }
                } else null

            val removeDownloadCallback: (() -> Unit)? =
                if (isDownloaded) {
                    { offlineViewModel.removeOfflineTrack(song.id) }
                } else null

            val cancelDownloadCallback: (() -> Unit)? =
                if (isDownloading) {
                    { offlineViewModel.cancelDownload(song.id) }
                } else null

            SongBottomSheet(
                song = song,
                onDismiss = {
                    showSongBottomSheet = false
                    selectedSong = null
                },
                onPlayNext = { playbackViewModel.playNext(song) },
                onAddToQueue = { playbackViewModel.addToQueue(song) },
                onGoToAlbum = {
                    val albumId = song.albumId
                    if (albumId != null) {
                        onNavigateToAlbum(albumId)
                    } else {
                        Toast.makeText(context, "Album info unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                onGoToArtist = {
                    val artistId = song.artistId
                    if (artistId != null) {
                        onNavigateToArtist(artistId)
                    } else {
                        Toast.makeText(context, "Artist info unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                onDownloadClick = downloadAction,
                downloadLabel = "Download offline (${selectedQuality.toDownloadLabel()})",
                onCancelDownload = cancelDownloadCallback,
                onRemoveDownload = removeDownloadCallback
            )
        }

        if (showAlbumBottomSheet && selectedAlbum != null) {
            val album = selectedAlbum!!
            AlbumBottomSheet(
                album = album,
                onDismiss = {
                    showAlbumBottomSheet = false
                    selectedAlbum = null
                },
                onPlay = { playbackViewModel.playAlbum(album.id) },
                onPlayNext = { playbackViewModel.playAlbumNext(album.id) },
                onAddToQueue = { playbackViewModel.addAlbumToQueue(album.id) },
                onNavigateToAlbum = { onNavigateToAlbum(album.id) },
                onNavigateToArtist = album.artistId?.let { artistId ->
                    {
                        onNavigateToArtist(artistId)
                    }
                }
            )
        }

        if (showPlaylistBottomSheet && selectedPlaylist != null) {
            val playlist = selectedPlaylist!!
            PlaylistBottomSheet(
                playlist = playlist,
                onDismiss = {
                    showPlaylistBottomSheet = false
                    selectedPlaylist = null
                },
                onPlay = { playbackViewModel.playPlaylist(playlist.id) },
                onPlayNext = { playbackViewModel.playPlaylistNext(playlist.id) },
                onAddToQueue = { playbackViewModel.addPlaylistToQueue(playlist.id) },
                onNavigateToPlaylist = { onNavigateToPlaylist(playlist.id) }
            )
        }
    }
}

private fun AudioQuality.toDownloadLabel(): String = when (this) {
    AudioQuality.LOW -> "128 kbps (Opus)"
    AudioQuality.MEDIUM -> "320 kbps (Opus)"
    AudioQuality.HIGH -> "Lossless (FLAC)"
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
