package com.shirou.shibamusic.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * FAB that hides on scroll
 */
@Composable
fun HideOnScrollFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    extended: Boolean = true
) {
    var previousScrollOffset by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    
    // Observe scroll state
    LaunchedEffect(listState?.firstVisibleItemScrollOffset, gridState?.firstVisibleItemScrollOffset) {
        val currentOffset = listState?.firstVisibleItemScrollOffset 
            ?: gridState?.firstVisibleItemScrollOffset 
            ?: 0
            
        visible = when {
            currentOffset == 0 -> true
            currentOffset < previousScrollOffset -> true // Scrolling up
            currentOffset > previousScrollOffset + 50 -> false // Scrolling down
            else -> visible
        }
        
        previousScrollOffset = currentOffset
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ) + fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        if (extended) {
            ExtendedFloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                icon = { Icon(icon, contentDescription = null) },
                text = { Text(text) }
            )
        } else {
            FloatingActionButton(
                onClick = onClick,
                modifier = modifier
            ) {
                Icon(icon, contentDescription = text)
            }
        }
    }
}

/**
 * Simple FAB for common actions
 */
@Composable
fun PlayAllFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null
) {
    HideOnScrollFAB(
        onClick = onClick,
        icon = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
        text = "Play all",
        modifier = modifier.padding(16.dp),
        listState = listState,
        extended = true
    )
}

@Composable
fun ShuffleFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null
) {
    HideOnScrollFAB(
        onClick = onClick,
        icon = androidx.compose.material.icons.Icons.Rounded.Shuffle,
        text = "Shuffle",
        modifier = modifier.padding(16.dp),
        listState = listState,
        extended = false
    )
}
