package com.shibamusic.data.model

enum class AudioQuality(val codec: AudioCodec, val fileExtension: String) {
    LOW(AudioCodec.OPUS, "opus"),
    MEDIUM(AudioCodec.OPUS, "opus"),
    HIGH(AudioCodec.FLAC, "flac");

    fun getTranscodeFormat(): String {
        return when (this) {
            LOW -> "opus"
            MEDIUM -> "opus"
            HIGH -> "raw" // FLAC is raw
        }
    }

    fun getBitrateString(): String {
        return when (this) {
            LOW -> "128k"
            MEDIUM -> "320k"
            HIGH -> "0" // No bitrate limit for FLAC
        }
    }
}

enum class AudioCodec(val displayName: String, val mimeType: String) {
    OPUS("Opus", "audio/opus"),
    FLAC("FLAC", "audio/flac")
}