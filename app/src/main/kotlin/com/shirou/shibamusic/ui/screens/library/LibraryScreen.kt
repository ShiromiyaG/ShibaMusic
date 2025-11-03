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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import com.shirou.shibamusic.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shirou.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.ui.component.NavigationTitle
import com.shirou.shibamusic.ui.component.SongBottomSheet
import com.shirou.shibamusic.ui.component.AlbumBottomSheet
import com.shirou.shibamusic.ui.component.PlaylistBottomSheet
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.AlbumSortOption
import com.shirou.shibamusic.ui.viewmodel.ArtistSortOption
import com.shirou.shibamusic.ui.viewmodel.LibraryAlbumsViewModel
import com.shirou.shibamusic.ui.viewmodel.LibraryArtistsViewModel
import com.shirou.shibamusic.ui.viewmodel.LibraryPlaylistsViewModel
import com.shirou.shibamusic.ui.viewmodel.LibrarySongsViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaylistSortOption
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import com.shirou.shibamusic.ui.viewmodel.SongSortOption
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
    offlineViewModel: OfflineViewModel = hiltViewModel(),
    contentBottomPadding: Dp = 0.dp
) {
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val initialTab = remember {
        Preferences.getLibraryLastTab()
            ?.let { stored -> runCatching { LibraryTab.valueOf(stored) }.getOrNull() }
            ?: LibraryTab.SONGS
    }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
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
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        Preferences.setLibraryLastTab(selectedTab.name)
    }
    
    var songsViewModel by remember { mutableStateOf<LibrarySongsViewModel?>(null) }
    var albumsViewModel by remember { mutableStateOf<LibraryAlbumsViewModel?>(null) }
    var artistsViewModel by remember { mutableStateOf<LibraryArtistsViewModel?>(null) }
    var playlistsViewModel by remember { mutableStateOf<LibraryPlaylistsViewModel?>(null) }

    if (selectedTab == LibraryTab.SONGS && songsViewModel == null) {
        songsViewModel = hiltViewModel()
    } else if (selectedTab == LibraryTab.ALBUMS && albumsViewModel == null) {
        albumsViewModel = hiltViewModel()
    } else if (selectedTab == LibraryTab.ARTISTS && artistsViewModel == null) {
        artistsViewModel = hiltViewModel()
    } else if (selectedTab == LibraryTab.PLAYLISTS && playlistsViewModel == null) {
        playlistsViewModel = hiltViewModel()
    }
    
    // Column com TopBar e Tabs integrados
    Column(modifier = modifier.fillMaxSize()) {
        // TopBar
        TopAppBar(
            title = {
                NavigationTitle(title = stringResource(R.string.nav_library))
            },
            actions = {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = stringResource(R.string.cd_sort)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        when (selectedTab) {
                            LibraryTab.SONGS -> songsViewModel?.let { vm ->
                                val songsState by vm.uiState.collectAsStateWithLifecycle()
                                SongSortOption.values().forEach { option ->
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
                                AlbumSortOption.values().forEach { option ->
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
                                ArtistSortOption.values().forEach { option ->
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
                                PlaylistSortOption.values().forEach { option ->
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
                        contentDescription = stringResource(R.string.cd_settings)
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
                    text = { Text(stringResource(tab.titleResId)) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.titleResId)
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
                        activeDownloads = activeDownloadMap,
                        contentBottomPadding = contentBottomPadding
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
                        },
                        contentBottomPadding = contentBottomPadding
                    )
                }
                LibraryTab.ARTISTS -> artistsViewModel?.let { vm ->
                    LibraryArtistsScreen(
                        viewModel = vm,
                        onArtistClick = { artist ->
                            onNavigateToArtist(artist.id)
                        },
                        onArtistPlay = { artist ->
                            playbackViewModel.playArtist(artist.id)
                        },
                        contentBottomPadding = contentBottomPadding
                    )
                }
                LibraryTab.PLAYLISTS -> playlistsViewModel?.let { vm ->
                    LibraryPlaylistsScreen(
                        viewModel = vm,
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylist(playlist.id)
                        },
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                        onPlaylistPlay = { playlist ->
                            playbackViewModel.playPlaylist(playlist.id)
                        },
                        onPlaylistMenuClick = { playlist ->
                            selectedPlaylist = playlist
                            showPlaylistBottomSheet = true
                        },
                        contentBottomPadding = contentBottomPadding
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

        if (showCreatePlaylistDialog) {
            playlistsViewModel?.let { vm ->
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onConfirm = { name, description ->
                        vm.createPlaylist(name.trim(), description.trim())
                        showCreatePlaylistDialog = false
                    }
                )
            }
        }
    }
}

private fun AudioQuality.toDownloadLabel(): String = when (this) {
    AudioQuality.LOW -> "128 kbps (Opus)"
    AudioQuality.MEDIUM -> "320 kbps (Opus)"
    AudioQuality.HIGH -> "Lossless (FLAC)"
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_create_playlist)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.playlist_description_label)) },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, description)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}

/**
 * Library tabs enum
 */
enum class LibraryTab(
    val titleResId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    SONGS(R.string.tab_songs, Icons.Rounded.MusicNote),
    ALBUMS(R.string.tab_albums, Icons.Rounded.Album),
    ARTISTS(R.string.tab_artists, Icons.Rounded.Person),
    PLAYLISTS(R.string.tab_playlists, Icons.Rounded.QueueMusic)
}
