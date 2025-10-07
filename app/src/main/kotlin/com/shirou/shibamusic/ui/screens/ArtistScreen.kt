package com.shirou.shibamusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.*

/**
 * Artist Detail Screen
 * Shows artist image, popular songs, and albums
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artist: ArtistDetailModel,
    popularSongs: List<SongItem>,
    albums: List<AlbumItem>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Artist Header with Image and Stats
            item {
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
                    // Circular Artist Image
                    Card(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        AsyncImage(
                            model = artist.imageUrl,
                            contentDescription = artist.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Artist Name
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${artist.albumCount} albums",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${artist.songCount} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        
                        OutlinedButton(
                            onClick = onShuffleClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }
            
            // Popular Songs Section
            if (popularSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Popular Songs",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(popularSongs.take(5), key = { it.id }) { song ->
                    SongListItem(
                        title = song.title,
                        artist = song.artistName,
                        album = song.albumName,
                        thumbnailUrl = song.albumArtUrl,
                        isPlaying = currentSongId == song.id && isPlaying,
                        onClick = { onSongClick(song) },
                        onMoreClick = {
                            selectedSong = song
                            showBottomSheet = true
                        }
                    )
                }
            }
            
            // Albums Section
            if (albums.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Albums",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(albums, key = { it.id }) { album ->
                    AlbumListItem(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Bottom Sheet for Song Menu
    if (showBottomSheet && selectedSong != null) {
        SongBottomSheet(
            song = selectedSong!!,
            onDismiss = { showBottomSheet = false },
            onPlayNext = { /* TODO */ },
            onAddToQueue = { /* TODO */ },
            onGoToAlbum = { /* TODO */ },
            onGoToArtist = { },
            onShare = { /* TODO */ }
        )
    }
}

/**
 * Album List Item for Artist Screen
 */
@Composable
private fun AlbumListItem(
    album: AlbumItem,
    onClick: () -> Unit,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Thumbnail
            Card(
                modifier = Modifier.size(56.dp)
            ) {
                AsyncImage(
                    model = album.albumArtUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Album Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = album.year?.toString() ?: "Unknown year",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Song Count
            Text(
                text = "${album.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

