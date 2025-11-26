package com.shirou.shibamusic.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shirou.shibamusic.helper.ThemeHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        // Add other keys as needed
    }

    val theme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[Keys.THEME] ?: ThemeHelper.DEFAULT_MODE
        }

    val appLanguage: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[Keys.APP_LANGUAGE]
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME] = theme
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_LANGUAGE] = language
        }
    }
}
