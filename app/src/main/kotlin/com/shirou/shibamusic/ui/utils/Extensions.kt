package com.shirou.shibamusic.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Utility extensions for formatting time
 */
object TimeUtils {
    /**
     * Convert milliseconds to MM:SS or HH:MM:SS format
     */
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%d:%02d", minutes, seconds % 60)
        }
    }
    
    /**
     * Convert milliseconds to short format (e.g., "3:45")
     */
    fun formatDurationShort(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        return String.format("%d:%02d", minutes, seconds % 60)
    }
    
    /**
     * Convert seconds to readable format (e.g., "3 minutes")
     */
    fun formatDurationReadable(seconds: Long): String {
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""}"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
            else -> "$seconds second${if (seconds != 1L) "s" else ""}"
        }
    }
}

/**
 * String extensions for Compose
 */
fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Format file size to human readable format
 */
fun Long.formatFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$this B"
    }
}

/**
 * Convert Dp to Px
 */
@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

/**
 * Convert Px to Dp
 */
@Composable
fun Int.toDp(): Dp {
    return with(LocalDensity.current) { this@toDp.toDp() }
}

/**
 * Pluralize string based on count
 */
fun String.pluralize(count: Int): String {
    return if (count == 1) this else "${this}s"
}

/**
 * Format track count (e.g., "1 song", "5 songs")
 */
fun formatTrackCount(count: Int): String {
    return "$count ${"track".pluralize(count)}"
}

/**
 * Format album count
 */
fun formatAlbumCount(count: Int): String {
    return "$count ${"album".pluralize(count)}"
}

/**
 * Truncate string with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        "${this.substring(0, maxLength)}..."
    } else {
        this
    }
}
