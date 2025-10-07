package com.shirou.shibamusic.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.Html
import android.util.Log
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.subsonic.models.Child
import java.text.CharacterIterator
import java.text.DecimalFormat
import java.text.StringCharacterIterator
import java.util.Locale
import kotlin.math.absoluteValue

object MusicUtil {
    private const val TAG = "MusicUtil"

    fun getStreamUri(id: String): Uri {
        val client = App.getSubsonicClientInstance(false)
        val params = client.params
        val uri = StringBuilder()

        uri.append(client.url)
        uri.append("stream")

        params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
        params["p"]?.let { uri.append("&p=").append(it) }
        params["s"]?.let { uri.append("&s=").append(it) }
        params["t"]?.let { uri.append("&t=").append(it) }
        params["v"]?.let { uri.append("&v=").append(it) }
        params["c"]?.let { uri.append("&c=").append(it) }

        if (!Preferences.isServerPrioritized()) {
            uri.append("&maxBitRate=").append(getBitratePreference())
            uri.append("&format=").append(getTranscodingFormatPreference())
        }
        if (Preferences.askForEstimateContentLength()) {
            uri.append("&estimateContentLength=true")
        }

        uri.append("&id=").append(id)

        val result = uri.toString().let(Uri::parse)
        Log.d(TAG, "getStreamUri: $result")
        return result
    }

    fun getDownloadUri(id: String): Uri {
        val download: Download? = DownloadRepository().getDownload(id)
        val result = download?.downloadUri
            ?.takeIf { it.isNotEmpty() }
            ?.let(Uri::parse)
            ?: run {
                val client = App.getSubsonicClientInstance(false)
                val params = client.params
                StringBuilder().apply {
                    append(client.url)
                    append("download")

                    params["u"]?.let { append("?u=").append(Util.encode(it)) }
                    params["p"]?.let { append("&p=").append(it) }
                    params["s"]?.let { append("&s=").append(it) }
                    params["t"]?.let { append("&t=").append(it) }
                    params["v"]?.let { append("&v=").append(it) }
                    params["c"]?.let { append("&c=").append(it) }

                    append("&id=").append(id)
                }.toString().let(Uri::parse)
            }

        Log.d(TAG, "getDownloadUri: $result")
        return result
    }

    fun getTranscodedDownloadUri(id: String): Uri {
        val client = App.getSubsonicClientInstance(false)
        val params = client.params
        val uri = StringBuilder()

        uri.append(client.url)
        uri.append("stream")

        params["u"]?.let { uri.append("?u=").append(Util.encode(it)) }
        params["p"]?.let { uri.append("&p=").append(it) }
        params["s"]?.let { uri.append("&s=").append(it) }
        params["t"]?.let { uri.append("&t=").append(it) }
        params["v"]?.let { uri.append("&v=").append(it) }
        params["c"]?.let { uri.append("&c=").append(it) }

        if (!Preferences.isServerPrioritizedInTranscodedDownload()) {
            uri.append("&maxBitRate=").append(getBitratePreferenceForDownload())
            uri.append("&format=").append(getTranscodingFormatPreferenceForDownload())
        }

        uri.append("&id=").append(id)

        val result = uri.toString().let(Uri::parse)
        Log.d(TAG, "getTranscodedDownloadUri: $result")
        return result
    }

    fun getReadableDurationString(duration: Long?, millis: Boolean): String {
        val length = duration ?: 0L
        val (minutes, seconds) = if (millis) {
            (length / 1000) / 60 to (length / 1000) % 60
        } else {
            length / 60 to length % 60
        }

        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        } else {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, remainingMinutes, seconds)
        }
    }

    fun getReadableDurationString(duration: Int?, millis: Boolean): String {
        return getReadableDurationString(duration?.toLong(), millis)
    }

    fun getReadableAudioQualityString(child: Child): String {
        if (!Preferences.showAudioQuality() || child.bitrate == null) return ""

        val bitDepthPart = child.bitDepth?.takeIf { it != 0 }?.let { depth ->
            val sample = child.samplingRate?.let { rate -> rate / 1000 } ?: ""
            "$depth/$sample"
        } ?: child.samplingRate?.let { rate ->
            DecimalFormat("0.#").format(rate / 1000.0) + "kHz"
        } ?: ""

        return buildString {
            append("• ")
            append(child.bitrate)
            append("kbps • ")
            append(bitDepthPart)
            if (bitDepthPart.isNotEmpty()) append(' ')
            append(child.suffix)
        }
    }

    fun getReadablePodcastDurationString(duration: Long): String {
        val minutes = duration / 60
        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d min", minutes)
        } else {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            String.format(Locale.getDefault(), "%d h %02d min", hours, remainingMinutes)
        }
    }

    fun getReadableTrackNumber(context: Context, trackNumber: Int?): String {
        return trackNumber?.toString() ?: context.getString(R.string.label_placeholder)
    }

    fun getReadableString(string: String?): String {
        return string?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString() } ?: ""
    }

    fun forceReadableString(string: String?): String {
        return string?.let {
            getReadableString(it)
                .replace("&#34;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "'")
                .replace("<a\\s+([^>]+)>((?:.(?!</a>))*.)</a>".toRegex(), "")
        } ?: ""
    }

    fun getReadableLyrics(string: String?): String {
        return string?.let {
            it.replace("&#34;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "'")
                .replace("&#xA;", "\n")
        } ?: ""
    }

    fun getReadableByteCount(bytes: Long): String {
        val absBytes = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else bytes.absoluteValue
        if (absBytes < 1024) {
            return "$bytes B"
        }

        var value = absBytes
        val iterator: CharacterIterator = StringCharacterIterator("KMGTPE")

        var shift = 40
        while (shift >= 0 && absBytes > 0xfffccccccccccccL shr shift) {
            value = value shr 10
            iterator.next()
            shift -= 10
        }

        value *= java.lang.Long.signum(bytes).toLong()
        return String.format(Locale.getDefault(), "%.1f %ciB", value / 1024.0, iterator.current())
    }

    fun passwordHexEncoding(plainPassword: String): String {
        return buildString {
            append("enc:")
            plainPassword.forEach { append(Integer.toHexString(it.code)) }
        }
    }

    fun getBitratePreference(): String {
        val connectivityManager = getConnectivityManager()
        val network = connectivityManager.activeNetwork
        val networkCapabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val audioTranscodeFormat = getTranscodingFormatPreference()
        if (audioTranscodeFormat == "raw" || network == null || networkCapabilities == null) {
            return "0"
        }

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Preferences.getMaxBitrateWifi()
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Preferences.getMaxBitrateMobile()
            else -> Preferences.getMaxBitrateWifi()
        }
    }

    fun getTranscodingFormatPreference(): String {
        val connectivityManager = getConnectivityManager()
        val network = connectivityManager.activeNetwork
        val networkCapabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        if (network == null || networkCapabilities == null) return "raw"

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Preferences.getAudioTranscodeFormatWifi()
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Preferences.getAudioTranscodeFormatMobile()
            else -> Preferences.getAudioTranscodeFormatWifi()
        }
    }

    fun getBitratePreferenceForDownload(): String {
        val audioTranscodeFormat = getTranscodingFormatPreferenceForDownload()
        if (audioTranscodeFormat == "raw") {
            return "0"
        }
        return Preferences.getBitrateTranscodedDownload()
    }

    fun getTranscodingFormatPreferenceForDownload(): String {
        return Preferences.getAudioTranscodeFormatTranscodedDownload()
    }

    fun limitPlayableMedia(toLimit: List<Child>, position: Int): List<Child> {
        return if (toLimit.isNotEmpty() && toLimit.size > Constants.PLAYABLE_MEDIA_LIMIT) {
            val from = if (position < Constants.PRE_PLAYABLE_MEDIA) 0 else position - Constants.PRE_PLAYABLE_MEDIA
            val to = minOf(from + Constants.PLAYABLE_MEDIA_LIMIT, toLimit.size)
            toLimit.subList(from, to)
        } else {
            toLimit
        }
    }

    fun getPlayableMediaPosition(toLimit: List<Child>, position: Int): Int {
        return if (toLimit.isNotEmpty() && toLimit.size > Constants.PLAYABLE_MEDIA_LIMIT) {
            minOf(position, Constants.PRE_PLAYABLE_MEDIA)
        } else {
            position
        }
    }

    fun ratingFilter(toFilter: List<Child>?): List<Child> {
        if (toFilter.isNullOrEmpty()) return emptyList()

        val filtered = toFilter.filter { child ->
            val rating = child.userRating
            rating == null || rating >= Preferences.getMinStarRatingAccepted()
        }

        if (toFilter is MutableList<Child>) {
            toFilter.clear()
            toFilter.addAll(filtered)
            return toFilter
        }

        return filtered
    }

    private fun getConnectivityManager(): ConnectivityManager {
        return App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}
