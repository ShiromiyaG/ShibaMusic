package com.shirou.shibamusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.EmptyPlaceholder
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.utils.TimeUtils

/**
 * Queue Screen
 * Shows current playing queue with drag-to-reorder support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queue: List<SongItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to current song when screen opens
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < queue.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSaveQueue) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = "Save queue as playlist"
                        )
                    }
                    IconButton(onClick = onClearQueue) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear queue"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (queue.isEmpty()) {
            EmptyPlaceholder(
                icon = Icons.Rounded.QueueMusic,
                text = "No songs in queue",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Queue stats
                QueueHeader(
                    songCount = queue.size,
                    totalDuration = queue.sumOf { it.duration }
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { index, song -> "${song.id}_$index" }
                    ) { index, song ->
                        QueueItem(
                            song = song,
                            position = index + 1,
                            isCurrentSong = index == currentIndex,
                            isPlaying = isPlaying && index == currentIndex,
                            onClick = { onSongClick(index) },
                            onRemoveClick = { onRemoveSong(index) }
                        )
                        
                        if (index < queue.lastIndex) {
                            Divider(modifier = Modifier.padding(start = 80.dp))
                        }
                    }
                    
                    // Bottom spacing for mini player
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * Queue header with stats
 */
@Composable
private fun QueueHeader(
    songCount: Int,
    totalDuration: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$songCount songs",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = TimeUtils.formatDuration(totalDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Individual queue item
 */
@Composable
private fun QueueItem(
    song: SongItem,
    position: Int,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isCurrentSong) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position or Playing Indicator
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentSong && isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Now Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isCurrentSong) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Paused",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Thumbnail
            Card(
                modifier = Modifier.size(48.dp)
            ) {
                AsyncImage(
                    model = song.getThumbnailUrl(),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Duration
            Text(
                text = TimeUtils.formatDurationShort(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Remove button (not visible for current song)
            AnimatedVisibility(
                visible = !isCurrentSong,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove from queue",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Drag handle indicator (for future drag-to-reorder feature)
            if (!isCurrentSong) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
