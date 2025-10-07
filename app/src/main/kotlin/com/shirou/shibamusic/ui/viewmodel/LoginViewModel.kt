package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.App
import com.shirou.shibamusic.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for Login Screen
 * Handles server authentication and configuration
 */
@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    /**
     * Attempt to login to server
     */
    fun login(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please fill all fields",
                isLoading = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                // Clean up URL
                val cleanUrl = serverUrl.trim().removeSuffix("/")
                
                // Validate URL format
                if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    _uiState.value = _uiState.value.copy(
                        error = "Server URL must start with http:// or https://",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Save credentials
                Preferences.setServer(cleanUrl)
                Preferences.setUser(username)
                Preferences.setPassword(password)
                
                // Refresh Subsonic client
                App.refreshSubsonicClient()
                
                // Try to ping server in IO thread
                val response = withContext(Dispatchers.IO) {
                    val subsonic = App.getSubsonicClientInstance(true)
                    subsonic.systemClient.ping().execute()
                }
                
                if (response.isSuccessful && response.body()?.subsonicResponse?.status == "ok") {
                    // Set serverId from server response or generate from URL
                    val serverId = response.body()?.subsonicResponse?.serverVersion ?: cleanUrl
                    Preferences.setServerId(serverId)
                    
                    // Success - mark as logged in (sync will happen in HomeViewModel)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccessful = true,
                        error = null
                    )
                } else {
                    // Server error
                    val error = response.body()?.subsonicResponse?.error
                    _uiState.value = _uiState.value.copy(
                        error = error?.message ?: "Failed to connect to server",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Connection failed: ${e.message ?: "Unknown error"}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Check if user is already logged in
     */
    fun isLoggedIn(): Boolean {
        val server = Preferences.getServer()
        val user = Preferences.getUser()
        val password = Preferences.getPassword()
        
        return !server.isNullOrBlank() && 
               !user.isNullOrBlank() && 
               !password.isNullOrBlank()
    }
}

/**
 * UI State for Login Screen
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val error: String? = null
)
