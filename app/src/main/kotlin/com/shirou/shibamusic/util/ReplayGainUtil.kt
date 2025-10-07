package com.shirou.shibamusic.util

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

import com.shirou.shibamusic.model.ReplayGain
import kotlin.math.pow

@OptIn(UnstableApi::class)
object ReplayGainUtil {
    private val tags = arrayOf("REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN", "R128_TRACK_GAIN", "R128_ALBUM_GAIN")

    fun setReplayGain(player: ExoPlayer, tracks: Tracks?) {
        val metadata = getMetadata(tracks)
        val gains = getReplayGains(metadata)

        applyReplayGain(player, gains)
    }

    private fun getMetadata(tracks: Tracks?): List<Metadata> {
        return tracks?.groups.orEmpty().flatMap { group ->
            group.mediaTrackGroup?.let { mediaTrackGroup ->
                (0 until mediaTrackGroup.length).mapNotNull { i ->
                    group.getTrackFormat(i)?.metadata
                }
            }.orEmpty()
        }
    }

    private fun getReplayGains(metadataList: List<Metadata>?): List<ReplayGain> {
        val gains = mutableListOf<ReplayGain>()

        metadataList.orEmpty().forEach { singleMetadata ->
            val length = singleMetadata.length()
            for (index in 0 until length) {
                val entry = singleMetadata.get(index)
                if (checkReplayGain(entry)) {
                    gains += setReplayGains(entry)
                }
            }
        }

        if (gains.isEmpty()) {
            gains.add(ReplayGain())
        }
        if (gains.size == 1) {
            gains.add(ReplayGain())
        }

        return gains
    }

    private fun checkReplayGain(entry: Metadata.Entry): Boolean {
        return tags.any { tag -> entry.toString().contains(tag) }
    }

    private fun setReplayGains(entry: Metadata.Entry): ReplayGain {
        return ReplayGain().apply {
            val entryString = entry.toString()
            if (entryString.contains(tags[0])) {
                trackGain = parseReplayGainTag(entry)
            }
            if (entryString.contains(tags[1])) {
                albumGain = parseReplayGainTag(entry)
            }
            if (entryString.contains(tags[2])) {
                trackGain = parseReplayGainTag(entry) / 256f
            }
            if (entryString.contains(tags[3])) {
                albumGain = parseReplayGainTag(entry) / 256f
            }
        }
    }

    private fun parseReplayGainTag(entry: Metadata.Entry): Float {
        return try {
            entry.toString().replace(Regex("[^\\d.-]"), "").toFloat()
        } catch (exception: NumberFormatException) {
            0f
        }
    }

    private fun applyReplayGain(player: ExoPlayer, gains: List<ReplayGain>) {
        if (!Preferences.isReplayGainEnabled() || gains.isEmpty()) {
            setNoReplayGain(player)
            return
        }

        when (Preferences.getReplayGainMode()) {
            "track" -> setTrackReplayGain(player, gains)
            "album" -> setAlbumReplayGain(player, gains)
            else -> setTrackReplayGain(player, gains)
        }
    }

    private fun setNoReplayGain(player: ExoPlayer) {
        setReplayGain(player, 0f)
    }

    private fun setTrackReplayGain(player: ExoPlayer, gains: List<ReplayGain>) {
        val trackGain = gains[0].trackGain.takeIf { it != 0f } ?: gains[1].trackGain
        setReplayGain(player, trackGain.takeIf { it != 0f } ?: 0f)
    }

    private fun setAlbumReplayGain(player: ExoPlayer, gains: List<ReplayGain>) {
        val albumGain = gains[0].albumGain.takeIf { it != 0f } ?: gains[1].albumGain
        setReplayGain(player, albumGain.takeIf { it != 0f } ?: 0f)
    }

    private fun setAutoReplayGain(player: ExoPlayer, gains: List<ReplayGain>) {
        val albumGain = gains[0].albumGain.takeIf { it != 0f } ?: gains[1].albumGain
        val trackGain = gains[0].trackGain.takeIf { it != 0f } ?: gains[1].trackGain

        setReplayGain(player, albumGain.takeIf { it != 0f } ?: trackGain)
    }

    private fun areTracksConsecutive(player: ExoPlayer): Boolean {
        val currentMediaItem = player.currentMediaItem
        val currentMediaItemIndex = player.currentMediaItemIndex
        val pastMediaItem = if (currentMediaItemIndex > 0) player.getMediaItemAt(currentMediaItemIndex - 1) else null

        return currentMediaItem?.mediaMetadata?.albumTitle != null &&
                pastMediaItem?.mediaMetadata?.albumTitle != null &&
                pastMediaItem.mediaMetadata.albumTitle.toString() == currentMediaItem.mediaMetadata.albumTitle.toString()
    }

    private fun setReplayGain(player: ExoPlayer, gain: Float) {
        player.volume = 10f.pow(gain / 20f)
    }
}
