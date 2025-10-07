package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * ThumbnailComponent - Componente reutilizável para thumbnails de música
 */
@Composable
fun MusicThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder quando não há imagem
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Variante circular para o MiniPlayer
 */
@Composable
fun CircularMusicThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentDescription: String? = null
) {
    MusicThumbnail(
        imageUrl = imageUrl,
        modifier = modifier,
        size = size,
        shape = CircleShape,
        contentDescription = contentDescription
    )
}

/**
 * Thumbnail grande para o PlayerScreen
 */
@Composable
fun LargeMusicThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    contentDescription: String? = null
) {
    MusicThumbnail(
        imageUrl = imageUrl,
        modifier = modifier,
        size = size,
        shape = RoundedCornerShape(16.dp),
        contentDescription = contentDescription
    )
}

/**
 * Thumbnail com blur effect (para backgrounds)
 */
@Composable
fun BlurredMusicThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.3f
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f))
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = alpha
            )
        }
    }
}
