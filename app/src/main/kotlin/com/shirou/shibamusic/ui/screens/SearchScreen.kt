package com.shirou.shibamusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.ListItemPlaceholder
import com.shirou.shibamusic.ui.model.*

/**
 * Search Screen
 * Shows search bar and results categorized by type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: SearchResults,
    isSearching: Boolean,
    currentSongId: String?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumPlay: (AlbumItem) -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onArtistPlay: (ArtistItem) -> Unit,
    onClearQuery: () -> Unit,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    downloadedSongIds: Set<String>,
    activeDownloads: Map<String, com.shirou.shibamusic.data.model.DownloadProgress>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SearchTab.ALL) }
    
    // Column com TopBar e Search Bar integrados
    Column(modifier = modifier.fillMaxSize()) {
        // TopBar
        TopAppBar(
            title = {
                Text("Search")
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = WindowInsets(0.dp)
        )
        
        // Search Bar
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { /* Search triggered on text change */ },
            placeholder = "Search songs, albums, artists...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (errorMessage != null) {
            SearchErrorBanner(
                message = errorMessage,
                onDismiss = onDismissError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        
        // Tabs
        if (query.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                SearchTab.values().forEach { tab ->
                    val count = when (tab) {
                        SearchTab.ALL -> searchResults.totalCount()
                        SearchTab.SONGS -> searchResults.songs.size
                        SearchTab.ALBUMS -> searchResults.albums.size
                        SearchTab.ARTISTS -> searchResults.artists.size
                    }
                    
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text("${tab.displayName} ($count)")
                        }
                    )
                }
            }
        }
        
        // Conteúdo
        when {
            query.isEmpty() -> {
                SearchEmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            }
            isSearching -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize()
                ) {
                    items(8) {
                        ListItemPlaceholder()
                    }
                }
            }
            searchResults.isEmpty() -> {
                EmptyPlaceholder(
                    icon = Icons.Rounded.SearchOff,
                    text = "No results found for \"$query\"",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                SearchResultsList(
                    selectedTab = selectedTab,
                    searchResults = searchResults,
                    currentSongId = currentSongId,
                    isPlaying = isPlaying,
                    onSongClick = onSongClick,
                    onAlbumClick = onAlbumClick,
                    onAlbumPlay = onAlbumPlay,
                    onArtistClick = onArtistClick,
                    onArtistPlay = onArtistPlay,
                    downloadedSongIds = downloadedSongIds,
                    activeDownloads = activeDownloads,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SearchErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss"
                )
            }
        }
    }
}

/**
 * Search results list
 */
@Composable
private fun SearchResultsList(
    selectedTab: SearchTab,
    searchResults: SearchResults,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (SongItem) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumPlay: (AlbumItem) -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onArtistPlay: (ArtistItem) -> Unit,
    downloadedSongIds: Set<String>,
    activeDownloads: Map<String, com.shirou.shibamusic.data.model.DownloadProgress>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        when (selectedTab) {
            SearchTab.ALL -> {
                // Songs
                if (searchResults.songs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Songs",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(
                        items = searchResults.songs.take(5),
                        key = { it.id }
                                    ) { song ->
                                        val isDownloaded = downloadedSongIds.contains(song.id)
                                        val downloadInfo = activeDownloads[song.id]
                                        SongListItem(
                                            title = song.title,
                                            artist = song.artistName,
                                            album = song.albumName,
                                            thumbnailUrl = song.getThumbnailUrl(),
                                            isPlaying = currentSongId == song.id && isPlaying,
                                            onClick = { onSongClick(song) },
                                            onMoreClick = { /* TODO */ },
                                            isDownloaded = isDownloaded,
                                            downloadInfo = downloadInfo
                                        )
                                    }                }
                
                // Albums
                if (searchResults.albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Albums",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(
                        items = searchResults.albums.take(5),
                        key = { it.id }
                    ) { album ->
                        AlbumListItem(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            onPlayClick = { onAlbumPlay(album) }
                        )
                    }
                }
                
                // Artists
                if (searchResults.artists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Artists",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(
                        items = searchResults.artists.take(5),
                        key = { it.id }
                    ) { artist ->
                        ArtistListItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) },
                            onPlayClick = { onArtistPlay(artist) }
                        )
                    }
                }
            }
            SearchTab.SONGS -> {
                items(
                    items = searchResults.songs,
                    key = { it.id }
                ) { song ->
                    val isDownloaded = downloadedSongIds.contains(song.id)
                    val downloadInfo = activeDownloads[song.id]
                    SongListItem(
                        title = song.title,
                        artist = song.artistName,
                        album = song.albumName,
                        thumbnailUrl = song.getThumbnailUrl(),
                        isPlaying = currentSongId == song.id && isPlaying,
                        onClick = { onSongClick(song) },
                        onMoreClick = { /* TODO */ },
                        isDownloaded = isDownloaded,
                        downloadInfo = downloadInfo
                    )
                }
            }
            SearchTab.ALBUMS -> {
                items(
                    items = searchResults.albums,
                    key = { it.id }
                ) { album ->
                    AlbumListItem(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        onPlayClick = { onAlbumPlay(album) }
                    )
                }
            }
            SearchTab.ARTISTS -> {
                items(
                    items = searchResults.artists,
                    key = { it.id }
                ) { artist ->
                    ArtistListItem(
                        artist = artist,
                        onClick = { onArtistClick(artist) },
                        onPlayClick = { onArtistPlay(artist) }
                    )
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Empty state when no query
 */
@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search your music library",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Songs, albums, and artists",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Album list item for search results
 */
@Composable
private fun AlbumListItem(
    album: AlbumItem,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Card(modifier = Modifier.size(56.dp)) {
                coil.compose.AsyncImage(
                    model = album.getThumbnailUrl(),
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onPlayClick != null) {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play album"
                    )
                }
            }
        }
    }
}

/**
 * Artist list item for search results
 */
@Composable
private fun ArtistListItem(
    artist: ArtistItem,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                coil.compose.AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${artist.albumCount} albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onPlayClick != null) {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play artist"
                    )
                }
            }
        }
    }
}

/**
 * Search tabs enum
 */
enum class SearchTab(val displayName: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}


