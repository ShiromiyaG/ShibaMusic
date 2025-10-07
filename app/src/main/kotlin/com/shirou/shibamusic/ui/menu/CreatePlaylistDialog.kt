package com.shirou.shibamusic.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog for creating a new playlist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Card {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Create Playlist",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Playlist Name
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                        isNameError = it.isBlank()
                    },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("Enter playlist name") },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text("Playlist name cannot be empty")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description (optional)
                OutlinedTextField(
                    value = playlistDescription,
                    onValueChange = { playlistDescription = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add a description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                onCreate(playlistName.trim(), playlistDescription.trim())
                                onDismiss()
                            } else {
                                isNameError = true
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

/**
 * Simple version with just name input
 */
@Composable
fun SimpleCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist Name") },
                placeholder = { Text("Enter playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onCreate(playlistName.trim())
                        onDismiss()
                    }
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
