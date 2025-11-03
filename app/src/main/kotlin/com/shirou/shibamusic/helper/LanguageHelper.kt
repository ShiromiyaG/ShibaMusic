package com.shirou.shibamusic.helper

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.shirou.shibamusic.util.Preferences
import java.util.Locale

object LanguageHelper {

    const val SYSTEM_DEFAULT = "system"
    const val ENGLISH = "en"
    const val PORTUGUESE = "pt"
    const val SPANISH = "es"
    const val FRENCH = "fr"
    const val GERMAN = "de"
    const val ITALIAN = "it"
    const val KOREAN = "ko"
    const val POLISH = "pl"
    const val RUSSIAN = "ru"
    const val TURKISH = "tr"
    const val CHINESE = "zh"

    fun getLocaleFromLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            PORTUGUESE -> Locale("pt", "BR")
            SPANISH -> Locale("es", "ES")
            FRENCH -> Locale("fr", "FR")
            GERMAN -> Locale("de", "DE")
            ITALIAN -> Locale("it", "IT")
            KOREAN -> Locale("ko", "KR")
            POLISH -> Locale("pl", "PL")
            RUSSIAN -> Locale("ru", "RU")
            TURKISH -> Locale("tr", "TR")
            CHINESE -> Locale("zh", "CN")
            else -> Locale(languageCode)
        }
    }

    fun applyLanguage(languageCode: String?) {
        when (languageCode) {
            null, SYSTEM_DEFAULT -> {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
            else -> {
                val locale = getLocaleFromLanguageCode(languageCode)
                Locale.setDefault(locale)
                val localeList = LocaleListCompat.create(locale)
                AppCompatDelegate.setApplicationLocales(localeList)
            }
        }
    }

    fun getCurrentLanguage(): String {
        val savedLanguage = Preferences.getAppLanguage()
        if (savedLanguage != null) {
            return savedLanguage
        }
        return SYSTEM_DEFAULT
    }

    fun setLanguage(languageCode: String) {
        Preferences.setAppLanguage(languageCode)
        applyLanguage(languageCode)
    }

    fun getLanguageDisplayName(languageCode: String, context: Context): String {
        return when (languageCode) {
            SYSTEM_DEFAULT -> context.getString(com.shirou.shibamusic.R.string.settings_language_system)
            ENGLISH -> context.getString(com.shirou.shibamusic.R.string.settings_language_english)
            PORTUGUESE -> context.getString(com.shirou.shibamusic.R.string.settings_language_portuguese)
            else -> languageCode
        }
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            SYSTEM_DEFAULT to "settings_language_system",
            ENGLISH to "settings_language_english",
            PORTUGUESE to "settings_language_portuguese"
        )
    }

    fun wrapContext(baseContext: Context): Context {
        val languagePreference = Preferences.getAppLanguage()
        if (languagePreference.isNullOrBlank() || languagePreference == SYSTEM_DEFAULT) {
            return baseContext
        }

        val locale = getLocaleFromLanguageCode(languagePreference)
        Locale.setDefault(locale)

        val configuration = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.setLocale(locale)
        }
        configuration.setLayoutDirection(locale)

        return baseContext.createConfigurationContext(configuration)
    }
}
