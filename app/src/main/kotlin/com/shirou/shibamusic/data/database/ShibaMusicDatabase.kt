package com.shirou.shibamusic.data.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shirou.shibamusic.data.dao.OfflineTrackDao
import com.shirou.shibamusic.data.model.AudioCodec
import com.shirou.shibamusic.data.model.AudioQuality
import com.shirou.shibamusic.data.model.DownloadProgress
import com.shirou.shibamusic.data.model.DownloadStatus
import com.shirou.shibamusic.data.model.OfflineTrack
import java.util.*

/**
 * Banco de dados principal do ShibaMusic
 * Inclui entidades para funcionalidade offline com suporte a Opus
 */
@Database(
    entities = [
        OfflineTrack::class,
        DownloadProgress::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // Definir auto-migrações quando necessário
    ]
)
@TypeConverters(Converters::class)
abstract class ShibaMusicDatabase : RoomDatabase() {
    
    abstract fun offlineTrackDao(): OfflineTrackDao
    
    companion object {
        const val DATABASE_NAME = "shibamusic_database"
        
        /**
         * Migração da versão 1 para 2 (adicionando tabelas offline)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Cria tabela de tracks offline
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `offline_tracks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `localFilePath` TEXT NOT NULL,
                        `originalUrl` TEXT NOT NULL,
                        `coverArtPath` TEXT,
                        `downloadedAt` INTEGER NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `quality` TEXT NOT NULL,
                        `isComplete` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                """)
                
                // Cria tabela de progresso de download
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `download_progress` (
                        `trackId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `progress` REAL NOT NULL,
                        `bytesDownloaded` INTEGER NOT NULL,
                        `totalBytes` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`trackId`)
                    )
                """)
                
                // Cria índices para melhor performance
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_offline_tracks_artist` 
                    ON `offline_tracks` (`artist`)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_offline_tracks_album` 
                    ON `offline_tracks` (`album`)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_offline_tracks_downloadedAt` 
                    ON `offline_tracks` (`downloadedAt`)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_download_progress_status` 
                    ON `download_progress` (`status`)
                """)
            }
        }
        
        /**
         * Migração da versão 2 para 3 (adicionando campo codec)
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adiciona campo codec à tabela offline_tracks
                database.execSQL("""
                    ALTER TABLE `offline_tracks` 
                    ADD COLUMN `codec` TEXT NOT NULL DEFAULT 'OPUS'
                """)
                
                // Atualiza registros existentes baseado na qualidade
                database.execSQL("""
                    UPDATE `offline_tracks` 
                    SET `codec` = CASE 
                        WHEN `quality` = 'HIGH' THEN 'FLAC'
                        ELSE 'OPUS'
                    END
                """)
            }
        }
    }
}

/**
 * Type converters para tipos customizados do Room
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromAudioQuality(quality: AudioQuality): String {
        return quality.name
    }
    
    @TypeConverter
    fun toAudioQuality(quality: String): AudioQuality {
        return AudioQuality.valueOf(quality)
    }
    
    @TypeConverter
    fun fromAudioCodec(codec: AudioCodec): String {
        return codec.name
    }
    
    @TypeConverter
    fun toAudioCodec(codec: String): AudioCodec {
        return AudioCodec.valueOf(codec)
    }
    
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus {
        return DownloadStatus.valueOf(status)
    }
}