package com.shirou.shibamusic.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shirou.shibamusic.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.shirou.shibamusic.data.model.AudioQuality
import com.shirou.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import com.shirou.shibamusic.util.Preferences
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


/**
 * Playlist Detail Screen
 * Shows playlist cover, songs, and actions
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onUpdatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onAddSongs: () -> Unit,
    onRemoveSongFromPlaylist: (SongItem) -> Unit,
    onReorderSongs: (List<SongItem>) -> Unit,
    onSongGoToAlbum: (String) -> Unit = {},
    onSongGoToArtist: (String) -> Unit = {},
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp
) {
    var showMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val offlineViewModel: OfflineViewModel = hiltViewModel()
    val offlineTracks by offlineViewModel.offlineTracks.collectAsStateWithLifecycle()
    val activeDownloads by offlineViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadedSongIds = remember(offlineTracks) { offlineTracks.map { it.id }.toSet() }
    val activeDownloadMap = remember(activeDownloads) { activeDownloads.associateBy { it.trackId } }
    val context = LocalContext.current

    val localSongs = remember(playlist.id) { mutableStateListOf<SongItem>() }
    var pendingReorderDispatch by remember { mutableStateOf(false) }

    LaunchedEffect(playlist.id, songs) {
        pendingReorderDispatch = false
        localSongs.clear()
        localSongs.addAll(songs)
    }

    val listState = rememberLazyListState()

    // ⚠️ A sua versão usa 'lazyListState' e NÃO tem 'onDragEnd'
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            var moved = false
            localSongs.apply {
                val currentFromIndex = indexOfFirst { it.id == from.key }
                val currentToIndex = indexOfFirst { it.id == to.key }
                if (currentFromIndex == -1 || currentToIndex == -1 || currentFromIndex == currentToIndex) {
                    return@apply
                }
                val item = removeAt(currentFromIndex)
                val insertionIndex = if (currentFromIndex < currentToIndex) {
                    currentToIndex.coerceAtMost(size)
                } else {
                    currentToIndex.coerceIn(0, size)
                }
                add(insertionIndex, item)
                moved = true
            }
            if (moved) {
                pendingReorderDispatch = true
            }
        }
    )

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && pendingReorderDispatch) {
            pendingReorderDispatch = false
            onReorderSongs(localSongs.toList())
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playlist_edit_action)) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playlist_add_songs)) },
                                onClick = {
                                    showMenu = false
                                    showAddSongsDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playlist_delete_action)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        if (localSongs.isEmpty()) {
            EmptyPlaylistContent(
                playlist = playlist,
                onAddSongs = onAddSongs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = listState,
                contentPadding = PaddingValues(bottom = contentBottomPadding)
            ) {
    // Header
    item {
        PlaylistHeader(
            playlist = playlist,
            songCount = localSongs.size,
            onPlayClick = onPlayClick,
            onShuffleClick = onShuffleClick,
            onDownloadClick = onDownloadClick
        )
    }

    itemsIndexed(
        items = localSongs,
        key = { _, song -> song.id }
    ) { _, song ->
        ReorderableItem(
            state = reorderableState,
            key = song.id
        ) { _ ->
            SongListItem(
                title = song.title,
                artist = song.artistName,
                album = song.albumName,
                thumbnailUrl = song.albumArtUrl,
                isPlaying = currentSongId == song.id && isPlaying,
                onClick = { onSongClick(song) },
                onMoreClick = { selectedSong = song; showBottomSheet = true },
                isDownloaded = downloadedSongIds.contains(song.id),
                downloadInfo = activeDownloadMap[song.id],
                dragHandle = { iconModifier ->
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        modifier = iconModifier.draggableHandle()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    // .animateItemPlacement() // Commented as per code
            )
        }
    }

    item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
}


    // Bottom Sheet for Song Menu
    if (showBottomSheet && selectedSong != null) {
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
                        album = song.albumName ?: playlist.name,
                        duration = song.duration,
                        coverArtUrl = song.albumArtUrl,
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
                showBottomSheet = false
                selectedSong = null
            },
            onPlayNext = { playbackViewModel.playNext(song) },
            onAddToQueue = { playbackViewModel.addToQueue(song) },
            onGoToAlbum = {
                val albumId = song.albumId
                if (albumId != null) {
                    onSongGoToAlbum(albumId)
                } else {
                    Toast.makeText(context, "Album info unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            onGoToArtist = {
                val artistId = song.artistId
                if (artistId != null) {
                    onSongGoToArtist(artistId)
                } else {
                    Toast.makeText(context, "Artist info unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            onDownloadClick = downloadAction,
            downloadLabel = stringResource(R.string.artist_download_offline, selectedQuality.toDownloadLabel()),
            onCancelDownload = cancelDownloadCallback,
            onRemoveDownload = removeDownloadCallback,
            showRemoveFromPlaylist = true,
            onRemoveFromPlaylist = {
                onRemoveSongFromPlaylist(song)
                showBottomSheet = false
                selectedSong = null
            }
        )
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            playlist = playlist,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, description ->
                onUpdatePlaylist(name.trim(), description.trim())
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.playlist_delete)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePlaylist()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    if (showAddSongsDialog) {
        AddSongsDialog(
            onDismiss = { showAddSongsDialog = false },
            onConfirm = {
                onAddSongs()
                showAddSongsDialog = false
            }
        )
    }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AddSongsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val viewModel: com.shirou.shibamusic.ui.viewmodel.LibrarySongsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val songs: LazyPagingItems<SongItem> = viewModel.songs.collectAsLazyPagingItems()
    val selectedSongs = remember { mutableStateListOf<SongItem>() }
    val refreshState = songs.loadState.refresh
    val initialLoading = refreshState is LoadState.Loading && songs.itemCount == 0
    val initialError = refreshState as? LoadState.Error

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.playlist_add_songs)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.settings_close))
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onConfirm,
                            enabled = selectedSongs.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.playlist_add_count, selectedSongs.size))
                        }
                    }
                )

                when {
                    initialLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    initialError != null && songs.itemCount == 0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = initialError.error.localizedMessage
                                    ?: "Failed to load songs.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = songs::retry) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = songs.itemCount,
                                key = { index -> songs[index]?.id ?: index },
                                contentType = { "song" }
                            ) { index ->
                                val song = songs[index] ?: return@items
                                val isSelected = selectedSongs.any { it.id == song.id }

                                Surface(
                                    onClick = {
                                        if (isSelected) {
                                            selectedSongs.removeAll { it.id == song.id }
                                        } else {
                                            selectedSongs.add(song)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = song.artistName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
private fun EditPlaylistDialog(
    playlist: PlaylistItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var description by remember { mutableStateOf(playlist.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_edit)) },
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
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
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
 * Playlist header with cover and actions
 */
@Composable
private fun PlaylistHeader(
    playlist: PlaylistItem,
    songCount: Int,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playlist Cover
        if (playlist.thumbnailUrl != null) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = playlist.name,
                modifier = Modifier
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (playlist.thumbnailUrl == null) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Playlist Name
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Description (if available)
        if (!playlist.description.isNullOrBlank()) {
            Text(
                text = playlist.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Song count
        Text(
            text = stringResource(R.string.label_songs_count, songCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_playlist)
                )
            }
            OutlinedIconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle_playlist)
                )
            }
            OutlinedIconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.cd_download_playlist)
                )
            }
            OutlinedIconButton(
                onClick = onEditClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.cd_edit_playlist)
                )
            }
        }
    }
}

/**
 * Empty playlist state
 */
@Composable
private fun EmptyPlaylistContent(
    playlist: PlaylistItem,
    onAddSongs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playlist header (minimal)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }

        // Empty state
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_no_playlist_songs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.empty_add_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddSongs) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.playlist_add_songs))
            }
        }
    }
}



