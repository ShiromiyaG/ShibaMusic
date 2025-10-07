package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Interface for sort options
 */
interface SortOption {
    val displayName: String
}

/**
 * Sort header with dropdown
 * Now generic to work with any SortOption enum
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortHeader(
    sortOptions: List<T>,
    selectedOption: T,
    onSortChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Sort by",
    getDisplayName: (T) -> String
) where T : Enum<T> {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = { expanded = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getDisplayName(selectedOption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = getDisplayName(option),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if (option == selectedOption) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }
    }
}

/**
 * Filter chips row
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    filters: List<String>,
    selectedFilters: Set<String>,
    onFilterToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter in selectedFilters,
                onClick = { onFilterToggle(filter) },
                label = {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
