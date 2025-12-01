package com.shirou.shibamusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shirou.shibamusic.ui.viewmodel.LoginViewModel
import com.shirou.shibamusic.R

/**
 * Login Screen para configurar conexão com servidor Navidrome/Subsonic
 * Melhorado para funcionar corretamente com teclado visível
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var showSavedAccountsDialog by remember { mutableStateOf(false) }
    val savedProfiles = remember { com.shirou.shibamusic.util.Preferences.getSavedProfiles() }

    // Helper function for login
    val performLogin: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
            viewModel.login(serverUrl, username, password)
        }
    }

    // Show success and navigate
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            // Save profile on success
            val currentServer = com.shirou.shibamusic.util.Preferences.getServer()
            val currentUser = com.shirou.shibamusic.util.Preferences.getUser()
            val currentPass = com.shirou.shibamusic.util.Preferences.getPassword()
            val currentToken = com.shirou.shibamusic.util.Preferences.getToken()
            val currentSalt = com.shirou.shibamusic.util.Preferences.getSalt()
            
            if (currentServer != null && currentUser != null) {
                com.shirou.shibamusic.util.Preferences.saveProfile(
                    name = "$currentUser @ ${android.net.Uri.parse(currentServer).host}",
                    url = currentServer,
                    username = currentUser,
                    password = currentPass,
                    token = currentToken,
                    salt = currentSalt
                )
            }
            onLoginSuccess()
        }
    }
    
    if (showSavedAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showSavedAccountsDialog = false },
            title = { Text(stringResource(id = R.string.dialog_saved_accounts_title)) },
            text = {
                Column {
                    savedProfiles.forEach { profile ->
                        ListItem(
                            headlineContent = { Text(profile.username) },
                            supportingContent = { Text(profile.url) },
                            modifier = Modifier.clickable {
                                com.shirou.shibamusic.util.Preferences.switchToProfile(profile)
                                // Auto-fill fields
                                serverUrl = profile.url
                                username = profile.username
                                password = profile.password ?: ""
                                showSavedAccountsDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedAccountsDialog = false }) {
                    Text(stringResource(R.string.cd_close))
                }
            }
        )
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Importante: ajusta padding quando teclado aparece
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.login_setup_title)) },
                actions = {
                    if (savedProfiles.isNotEmpty()) {
                        IconButton(onClick = { showSavedAccountsDialog = true }) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = stringResource(id = R.string.dialog_saved_accounts_title))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(), // Padding para barra de navegação
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Logo/Icon com Card
                Card(
                    modifier = Modifier.size(120.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shiba_vector),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(id = R.string.login_setup_welcome),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = R.string.login_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Server URL Field
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text(stringResource(id = R.string.login_setup_server_url_label)) },
                            placeholder = { Text(stringResource(id = R.string.login_setup_server_url_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            isError = uiState.error != null && serverUrl.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        // Username Field
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(id = R.string.login_setup_username_label)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            isError = uiState.error != null && username.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(id = R.string.login_setup_password_label)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Rounded.Visibility
                                        } else {
                                            Icons.Rounded.VisibilityOff
                                        },
                                        contentDescription = if (passwordVisible) {
                                            stringResource(id = R.string.login_setup_hide_password)
                                        } else {
                                            stringResource(id = R.string.login_setup_show_password)
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { performLogin() }
                            ),
                            singleLine = true,
                            isError = uiState.error != null && password.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
                
                // Error message
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Login Button
                Button(
                    onClick = performLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = serverUrl.isNotBlank() && 
                             username.isNotBlank() && 
                             password.isNotBlank() && 
                             !uiState.isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.login_setup_button_connect),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Help text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.login_setup_help_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Extra space for better scrolling with keyboard
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
