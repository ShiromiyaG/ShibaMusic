package com.shirou.shibamusic

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.shirou.shibamusic.data.repository.SyncRepository
import com.shirou.shibamusic.helper.ThemeHelper
import com.shirou.shibamusic.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

/**
 * Application class for Shiba Music
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class ShibaMusicApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var syncRepositoryProvider: Provider<SyncRepository>

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply theme
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val themePref = sharedPreferences.getString(Preferences.THEME, ThemeHelper.DEFAULT_MODE) ?: ThemeHelper.DEFAULT_MODE
        ThemeHelper.applyTheme(themePref)
        
        // Initialize legacy App singleton for backward compatibility
        // This is needed because many parts of the code still use App.getInstance()
        App.initializeFromShibaMusicApplication(this)
        
        maybeStartInitialSync()
    }

    private fun maybeStartInitialSync() {
        val hasCredentials = Preferences.getServer() != null &&
            (Preferences.getPassword() != null ||
                    (Preferences.getToken() != null && Preferences.getSalt() != null))

        if (!hasCredentials) return

        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(2000)
            val repository = syncRepositoryProvider.get()
            repository.performInitialSync()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
