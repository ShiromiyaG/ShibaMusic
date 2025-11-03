package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shirou.shibamusic.R
import com.shirou.shibamusic.ui.model.*

/**
 * Bottom sheet menu item
 */
@Composable
fun BottomSheetMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

/**
 * Bottom sheet header with title and close button
 */
@Composable
fun BottomSheetHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generic bottom sheet with common menu items
 * Example usage for song menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongBottomSheet(
    song: SongItem,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    downloadLabel: String? = null,
    onCancelDownload: (() -> Unit)? = null,
    onRemoveDownload: (() -> Unit)? = null,
    showRemoveFromPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val actualDownloadLabel = downloadLabel ?: context.getString(R.string.label_save_offline)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            BottomSheetHeader(
                title = song.title,
                onDismiss = onDismiss
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Menu items
            BottomSheetMenuItem(
                icon = Icons.Rounded.PlayArrow,
                text = stringResource(R.string.bottom_sheet_play_next),
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )
            
            BottomSheetMenuItem(
                icon = Icons.Rounded.AddCircleOutline,
                text = stringResource(R.string.bottom_sheet_add_to_queue),
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            BottomSheetMenuItem(
                icon = Icons.Rounded.Album,
                text = stringResource(R.string.bottom_sheet_go_to_album),
                onClick = {
                    onGoToAlbum()
                    onDismiss()
                }
            )
            
            BottomSheetMenuItem(
                icon = Icons.Rounded.Person,
                text = stringResource(R.string.bottom_sheet_go_to_artist),
                onClick = {
                    onGoToArtist()
                    onDismiss()
                }
            )
            
            if (onDownloadClick != null || onCancelDownload != null || onRemoveDownload != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (onDownloadClick != null) {
                    BottomSheetMenuItem(
                        icon = Icons.Rounded.Download,
                        text = actualDownloadLabel,
                        onClick = {
                            onDownloadClick()
                            onDismiss()
                        }
                    )
                }

                if (onCancelDownload != null) {
                    BottomSheetMenuItem(
                        icon = Icons.Rounded.Cancel,
                        text = stringResource(R.string.bottom_sheet_cancel_download),
                        onClick = {
                            onCancelDownload()
                            onDismiss()
                        }
                    )
                }

                if (onRemoveDownload != null) {
                    BottomSheetMenuItem(
                        icon = Icons.Rounded.Delete,
                        text = stringResource(R.string.bottom_sheet_remove_download),
                        onClick = {
                            onRemoveDownload()
                            onDismiss()
                        }
                    )
                }
            }
            
            if (showRemoveFromPlaylist && onRemoveFromPlaylist != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                BottomSheetMenuItem(
                    icon = Icons.Rounded.RemoveCircleOutline,
                    text = stringResource(R.string.bottom_sheet_remove_from_playlist),
                    onClick = {
                        onRemoveFromPlaylist()
                        onDismiss()
                    }
                )
            }
            
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumBottomSheet(
    album: AlbumItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onNavigateToAlbum: () -> Unit,
    onNavigateToArtist: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            BottomSheetHeader(
                title = album.title,
                onDismiss = onDismiss
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            BottomSheetMenuItem(
                icon = Icons.Rounded.PlayArrow,
                text = stringResource(R.string.bottom_sheet_play_album),
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )

            BottomSheetMenuItem(
                icon = Icons.Rounded.PlaylistPlay,
                text = stringResource(R.string.bottom_sheet_play_next),
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            BottomSheetMenuItem(
                icon = Icons.Rounded.QueueMusic,
                text = stringResource(R.string.bottom_sheet_add_to_queue),
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            BottomSheetMenuItem(
                icon = Icons.Rounded.Album,
                text = stringResource(R.string.bottom_sheet_open_album),
                onClick = {
                    onNavigateToAlbum()
                    onDismiss()
                }
            )

            if (onNavigateToArtist != null) {
                BottomSheetMenuItem(
                    icon = Icons.Rounded.Person,
                    text = stringResource(R.string.bottom_sheet_go_to_artist),
                    onClick = {
                        onNavigateToArtist()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    playlist: PlaylistItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            BottomSheetHeader(
                title = playlist.name,
                onDismiss = onDismiss
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            BottomSheetMenuItem(
                icon = Icons.Rounded.PlayArrow,
                text = stringResource(R.string.bottom_sheet_play_playlist),
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )

            BottomSheetMenuItem(
                icon = Icons.Rounded.PlaylistPlay,
                text = stringResource(R.string.bottom_sheet_play_next),
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            BottomSheetMenuItem(
                icon = Icons.Rounded.QueueMusic,
                text = stringResource(R.string.bottom_sheet_add_to_queue),
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            BottomSheetMenuItem(
                icon = Icons.Rounded.List,
                text = stringResource(R.string.bottom_sheet_open_playlist),
                onClick = {
                    onNavigateToPlaylist()
                    onDismiss()
                }
            )
        }
    }
}
