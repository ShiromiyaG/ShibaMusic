package com.shibamusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shibamusic.data.model.AudioCodec
import com.shibamusic.data.model.AudioQuality

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
                    text = "Selecionar Qualidade",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Escolha a qualidade de download",
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
                                    text = "Opus é um codec de áudio moderno e eficiente, oferecendo melhor qualidade com menor tamanho de arquivo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Opus economiza até 40% de espaço comparado ao MP3",
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
                        Text("Cancelar")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onQualitySelected(selectedQuality) }
                    ) {
                        Text("Baixar")
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
                brush = androidx.compose.foundation.BorderStroke(
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
                        text = "~$estimatedSizeMB MB",
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
        AudioQuality.LOW -> "Opus 128kbps"
        AudioQuality.MEDIUM -> "Opus 320kbps"
        AudioQuality.HIGH -> "FLAC Lossless"
    }
}

@Composable
fun getQualityDescription(quality: AudioQuality): String {
    return when (quality) {
        AudioQuality.LOW -> "Boa qualidade, menor tamanho. Ideal para economizar espaço."
        AudioQuality.MEDIUM -> "Alta qualidade, tamanho equilibrado. Recomendado para a maioria dos usos."
        AudioQuality.HIGH -> "Qualidade lossless, arquivo maior. Para audiófilos."
    }
}

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