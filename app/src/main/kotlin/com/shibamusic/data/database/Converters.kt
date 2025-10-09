package com.shibamusic.data.database

import androidx.room.TypeConverter
import com.shibamusic.data.model.AudioCodec
import com.shibamusic.data.model.AudioQuality
import com.shibamusic.data.model.DownloadStatus
import java.util.Date

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
