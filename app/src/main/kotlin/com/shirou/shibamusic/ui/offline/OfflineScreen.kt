package com.shirou.shibamusic.ui.offline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shirou.shibamusic.R
import com.shirou.shibamusic.data.model.DownloadProgress
import com.shirou.shibamusic.data.model.DownloadStatus
import com.shirou.shibamusic.data.model.OfflineTrack
import com.shirou.shibamusic.repository.OfflineStorageInfo
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    modifier: Modifier = Modifier,
    viewModel: OfflineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val offlineTracks by viewModel.offlineTracks.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.error, uiState.message) {
        // Limpa mensagens após 3 segundos
        if (uiState.error != null || uiState.message != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabeçalho com informações de armazenamento
        OfflineStorageCard(
            storageInfo = uiState.storageInfo,
            onClearAll = { viewModel.clearAllOfflineData() },
            onVerifyIntegrity = { viewModel.verifyOfflineIntegrity() },
            onPlayAll = { viewModel.playAllOffline() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Downloads ativos
        if (activeDownloads.isNotEmpty()) {
            Text(
                text = stringResource(R.string.offline_downloads_in_progress),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            activeDownloads.forEach { download ->
                DownloadProgressCard(
                    downloadProgress = download,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Lista de músicas offline
        Text(
            text = stringResource(R.string.offline_tracks_count, offlineTracks.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (offlineTracks.isEmpty()) {
            EmptyOfflineState(
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn {
                items(offlineTracks) { track ->
                    OfflineTrackItem(
                        track = track,
                        isCurrentlyPlaying = currentTrack?.id == track.id && isPlaying,
                        onPlay = { viewModel.playOfflineTrack(track.id) },
                        onRemove = { viewModel.removeOfflineTrack(track.id) }
                    )
                }
            }
        }
    }
    
    // Loading overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // Snackbar para mensagens
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Mostrar snackbar de erro
        }
    }
    
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Mostrar snackbar de sucesso
        }
    }
}

@Composable
fun OfflineStorageCard(
    storageInfo: OfflineStorageInfo?,
    onClearAll: () -> Unit,
    onVerifyIntegrity: () -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.offline_storage_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            storageInfo?.let { info ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.offline_storage_info, info.trackCount))
                    Text(stringResource(R.string.offline_storage_size, info.totalSizeMB.roundToInt()))
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.offline_available_space, info.availableSpaceMB.roundToInt()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.offline_play_all))
                }
                
                OutlinedButton(
                    onClick = onVerifyIntegrity
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                }
                
                OutlinedButton(
                    onClick = onClearAll
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun DownloadProgressCard(
    downloadProgress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.offline_track_id, downloadProgress.trackId),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = when (downloadProgress.status) {
                        DownloadStatus.PENDING -> "Pendente"
                        DownloadStatus.DOWNLOADING -> "${(downloadProgress.progress * 100).roundToInt()}%"
                        DownloadStatus.COMPLETED -> "Concluído"
                        DownloadStatus.FAILED -> "Falhou"
                        DownloadStatus.PAUSED -> "Pausado"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { downloadProgress.progress },
                modifier = Modifier.fillMaxWidth()
            )
            
            downloadProgress.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun OfflineTrackItem(
    track: OfflineTrack,
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlay
            ) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentlyPlaying) "Pausar" else "Reproduzir",
                    tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = stringResource(R.string.offline_track_subtitle, track.artist, track.album),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${(track.fileSize / (1024 * 1024.0)).roundToInt()} MB • ${track.quality.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onRemove
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_remove),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun EmptyOfflineState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.empty_no_offline_music),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.empty_offline_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}