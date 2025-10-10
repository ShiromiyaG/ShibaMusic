package com.shirou.shibamusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.WindowInsets
import com.shibamusic.data.model.AudioQuality
import com.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.helper.ThemeHelper
import com.shirou.shibamusic.util.Preferences

/**
 * Settings Screen
 * Configurações do app: servidor, tema, cache, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSyncFromServer: () -> Unit = {},
    offlineViewModel: OfflineViewModel = hiltViewModel()
) {
    val serverUrl = remember { Preferences.getServer() ?: "Not configured" }
    val username = remember { Preferences.getUser() ?: "Not logged in" }
    var showClearOfflineDialog by remember { mutableStateOf(false) }
    var selectedDownloadQuality by remember { mutableStateOf(Preferences.getOfflineDownloadQuality()) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            // Server Section
            SettingsSection(title = stringResource(com.shirou.shibamusic.R.string.settings_server_title)) {
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_server_url_title),
                    subtitle = serverUrl,
                    onClick = {}
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Person,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_username_title),
                    subtitle = username,
                    onClick = {}
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                
                SettingsItem(
                    icon = Icons.Rounded.Sync,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_sync_title),
                    subtitle = stringResource(com.shirou.shibamusic.R.string.settings_sync_subtitle),
                    onClick = onSyncFromServer
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                
                SettingsItem(
                    icon = Icons.Rounded.Logout,
                    title = androidx.compose.ui.res.stringResource(com.shirou.shibamusic.R.string.settings_logout_title),
                    subtitle = "",
                    onClick = {
                        Preferences.setServer("")
                        Preferences.setUser("")
                        Preferences.setPassword("")
                        onNavigateToLogin()
                    }
                )
            }
            
            // Appearance Section
            SettingsSection(title = stringResource(com.shirou.shibamusic.R.string.settings_appearance_title)) {
                var currentTheme by remember { mutableStateOf(Preferences.getTheme()) }
                var showThemeDialog by remember { mutableStateOf(false) }
                
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = "Theme",
                    subtitle = when(currentTheme) {
                        ThemeHelper.LIGHT_MODE -> "Claro"
                        ThemeHelper.DARK_MODE -> "Escuro"
                        else -> "Mesmo do dispositivo"
                    },
                    onClick = { showThemeDialog = true }
                )
                
                if (showThemeDialog) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Tema") },
                        text = {
                            Column {
                                listOf(
                                    ThemeHelper.DEFAULT_MODE to "Mesmo do dispositivo",
                                    ThemeHelper.LIGHT_MODE to "Claro",
                                    ThemeHelper.DARK_MODE to "Escuro"
                                ).forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentTheme = value
                                                Preferences.setTheme(value)
                                                ThemeHelper.applyTheme(value)
                                                showThemeDialog = false
                                                (context as? android.app.Activity)?.recreate()
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentTheme == value,
                                            onClick = {
                                                currentTheme = value
                                                Preferences.setTheme(value)
                                                ThemeHelper.applyTheme(value)
                                                showThemeDialog = false
                                                (context as? android.app.Activity)?.recreate()
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
            
            // Playback Section
            SettingsSection(title = stringResource(com.shirou.shibamusic.R.string.settings_playback_title)) {
                var replayGainEnabled by remember { 
                    mutableStateOf(Preferences.isReplayGainEnabled())
                }
                
                SettingsItemWithSwitch(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_title),
                    subtitle = stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_subtitle),
                    checked = replayGainEnabled,
                    onCheckedChange = { checked ->
                        replayGainEnabled = checked
                        Preferences.setReplayGainEnabled(checked)
                    }
                )
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = replayGainEnabled,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    var replayGainMode by remember { 
                        mutableStateOf(Preferences.getReplayGainMode() ?: "track")
                    }
                    var showModeDialog by remember { mutableStateOf(false) }
                    
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        
                        SettingsItem(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_title),
                            subtitle = when(replayGainMode) {
                                "track" -> stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_track)
                                "album" -> stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_album)
                                else -> stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_track)
                            },
                            onClick = { showModeDialog = true }
                        )
                        
                        if (showModeDialog) {
                        AlertDialog(
                            onDismissRequest = { showModeDialog = false },
                            title = { Text(stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_dialog_title)) },
                            text = {
                                Column {
                                    listOf(
                                        "track" to stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_track),
                                        "album" to stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_album)
                                    ).forEach { (value, label) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    replayGainMode = value
                                                    Preferences.setReplayGainMode(value)
                                                    showModeDialog = false
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = replayGainMode == value,
                                                onClick = {
                                                    replayGainMode = value
                                                    Preferences.setReplayGainMode(value)
                                                    showModeDialog = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showModeDialog = false }) {
                                    Text(stringResource(com.shirou.shibamusic.R.string.settings_replay_gain_mode_dialog_cancel))
                                }
                            }
                        )
                        }
                    }
                }
            }
            
            // Cache Section
            SettingsSection(title = stringResource(com.shirou.shibamusic.R.string.settings_storage_title)) {
                SettingsItem(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Qualidade dos downloads offline",
                    subtitle = selectedDownloadQuality.toDownloadLabel(),
                    onClick = { showDownloadQualityDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                
                SettingsItem(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Limpar músicas offline",
                    subtitle = "Remove todos os arquivos baixados para uso offline",
                    onClick = { showClearOfflineDialog = true }
                )
            }

            if (showDownloadQualityDialog) {
                AlertDialog(
                    onDismissRequest = { showDownloadQualityDialog = false },
                    title = { Text("Qualidade dos downloads offline") },
                    text = {
                        Column {
                            AudioQuality.values().forEach { quality ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDownloadQuality = quality
                                            Preferences.setOfflineDownloadQuality(quality)
                                            showDownloadQualityDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedDownloadQuality == quality,
                                        onClick = {
                                            selectedDownloadQuality = quality
                                            Preferences.setOfflineDownloadQuality(quality)
                                            showDownloadQualityDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(quality.toDownloadLabel())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDownloadQualityDialog = false }) {
                            Text("Fechar")
                        }
                    }
                )
            }
            
            if (showClearOfflineDialog) {
                AlertDialog(
                    onDismissRequest = { showClearOfflineDialog = false },
                    title = { Text("Limpar músicas offline?") },
                    text = { Text("Todos os arquivos baixados serão removidos, incluindo versões antigas e novas do cache.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearOfflineDialog = false
                                offlineViewModel.clearAllOfflineData()
                                Toast.makeText(context, "Limpando músicas offline…", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Limpar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearOfflineDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            
            // About Section
            SettingsSection(title = stringResource(com.shirou.shibamusic.R.string.settings_about_title_section)) {
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_version_title_item),
                    subtitle = "1.0.0",
                    onClick = {}
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                
                SettingsItem(
                    icon = Icons.Rounded.Code,
                    title = stringResource(com.shirou.shibamusic.R.string.settings_github_title_item),
                    subtitle = stringResource(com.shirou.shibamusic.R.string.settings_github_subtitle),
                    onClick = { uriHandler.openUri("https://github.com/ShiromiyaG/ShibaMusic") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun AudioQuality.toDownloadLabel(): String = when (this) {
    AudioQuality.LOW -> "128 kbps (Opus)"
    AudioQuality.MEDIUM -> "320 kbps (Opus)"
    AudioQuality.HIGH -> "Lossless (FLAC)"
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
