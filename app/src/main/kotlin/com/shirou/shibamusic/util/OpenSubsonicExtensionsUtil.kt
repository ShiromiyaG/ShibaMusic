package com.shirou.shibamusic.util

import com.shirou.shibamusic.subsonic.models.OpenSubsonicExtension
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

object OpenSubsonicExtensionsUtil {

    private val openSubsonicExtensions: List<OpenSubsonicExtension>?
        get() = if (Preferences.isOpenSubsonic() && Preferences.getOpenSubsonicExtensions() != null) {
            Gson().fromJson(
                Preferences.getOpenSubsonicExtensions(),
                object : TypeToken<List<OpenSubsonicExtension>>() {}.type
            )
        } else {
            null
        }

    private fun getOpenSubsonicExtension(extensionName: String): OpenSubsonicExtension? {
        return openSubsonicExtensions?.find { it.name == extensionName }
    }

    val isTranscodeOffsetExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("transcodeOffset") != null

    val isFormPostExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("formPost") != null

    val isSongLyricsExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("songLyrics") != null
}
