package com.shirou.shibamusic.util

import com.shirou.shibamusic.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database initializer that populates the database with sample data on first launch
 * 
 * This should be called from the Application class onCreate()
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val repository: MusicRepositoryImpl
) {
    /**
     * Initialize database with sample data if empty
     * This is a fire-and-forget operation that runs on IO thread
     */
    fun initializeIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.insertSampleData()
            } catch (e: Exception) {
                // Log error but don't crash app
                println("DatabaseInitializer: Error inserting sample data: ${e.message}")
            }
        }
    }
    
    /**
     * Clear all data from database
     * Use with caution - this will delete everything!
     */
    suspend fun clearAllData() {
        repository.clearAllData()
    }
}
