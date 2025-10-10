package com.shirou.shibamusic.data.model

enum class AudioQuality(val codec: AudioCodec) {
    LOW(AudioCodec.OPUS),
    MEDIUM(AudioCodec.OPUS),
    HIGH(AudioCodec.FLAC);

    val fileExtension: String get() = codec.fileExtension

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

enum class AudioCodec(
    val displayName: String,
    val downloadMimeType: String,
    val playbackMimeType: String,
    val fileExtension: String
) {
    OPUS("Opus", "application/ogg", "audio/ogg", "ogg"),
    FLAC("FLAC", "audio/flac", "audio/flac", "flac")
}
