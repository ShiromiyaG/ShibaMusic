package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shirou.shibamusic.R
import com.shirou.shibamusic.data.model.AudioCodec
import com.shirou.shibamusic.data.model.AudioQuality

/**
 * Diálogo para seleção de qualidade de download
 * Destaca os benefícios do codec Opus
 */
@Composable
fun QualitySelectionDialog(
    onDismiss: () -> Unit,
    onQualitySelected: (AudioQuality) -> Unit,
    estimatedFileSizeMB: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedQuality by remember { mutableStateOf(AudioQuality.MEDIUM) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Cabeçalho
                Text(
                    text = stringResource(R.string.quality_selection_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(R.string.quality_selection_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Opções de qualidade
                AudioQuality.values().forEach { quality ->
                    QualityOption(
                        quality = quality,
                        isSelected = selectedQuality == quality,
                        estimatedSizeMB = calculateEstimatedSize(estimatedFileSizeMB, quality),
                        onSelect = { selectedQuality = quality },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (quality != AudioQuality.values().last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Informação sobre Opus
                if (selectedQuality == AudioQuality.LOW || selectedQuality == AudioQuality.MEDIUM) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = stringResource(R.string.quality_opus_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = stringResource(R.string.quality_opus_savings),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.quality_cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onQualitySelected(selectedQuality) }
                    ) {
                        Text(stringResource(R.string.quality_download))
                    }
                }
            }
        }
    }
}

@Composable
fun QualityOption(
    quality: AudioQuality,
    isSelected: Boolean,
    estimatedSizeMB: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ).brush
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Ícone do codec
            Icon(
                imageVector = when (quality.codec) {
                    AudioCodec.OPUS -> Icons.Default.HighQuality
                    AudioCodec.FLAC -> Icons.Default.AudioFile
                    else -> Icons.Default.MusicNote
                },
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título da qualidade
                Text(
                    text = getQualityDisplayName(quality),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Descrição
                Text(
                    text = getQualityDescription(quality),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                // Tamanho estimado
                if (estimatedSizeMB > 0) {
                    Text(
                        text = stringResource(R.string.label_estimated_size, estimatedSizeMB),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun getQualityDisplayName(quality: AudioQuality): String {
    return when (quality) {
        AudioQuality.LOW -> stringResource(R.string.quality_low)
        AudioQuality.MEDIUM -> stringResource(R.string.quality_medium)
        AudioQuality.HIGH -> stringResource(R.string.quality_high)
    }
}

@Composable
fun getQualityDescription(quality: AudioQuality): String {
    return when (quality) {
        AudioQuality.LOW -> stringResource(R.string.quality_low_description)
        AudioQuality.MEDIUM -> stringResource(R.string.quality_medium_description)
        AudioQuality.HIGH -> stringResource(R.string.quality_high_description)
    }
}

// Note: quality_low, quality_medium, quality_high are defined in strings_offline.xml
// quality_low_description, quality_medium_description, quality_high_description are defined in strings.xml

/**
 * Calcula o tamanho estimado do arquivo baseado na qualidade
 */
fun calculateEstimatedSize(baseSizeMB: Int, quality: AudioQuality): Int {
    if (baseSizeMB == 0) return 0
    
    return when (quality) {
        AudioQuality.LOW -> (baseSizeMB * 0.3).toInt() // Opus é muito eficiente
        AudioQuality.MEDIUM -> (baseSizeMB * 0.6).toInt() // Opus 320 ainda economiza espaço
        AudioQuality.HIGH -> (baseSizeMB * 1.5).toInt() // FLAC é maior que o original
    }
}