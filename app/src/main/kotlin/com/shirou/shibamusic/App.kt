package com.shirou.shibamusic

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.shirou.shibamusic.github.Github
import com.shirou.shibamusic.helper.ThemeHelper
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.SubsonicPreferences
import com.shirou.shibamusic.util.Preferences

/**
 * Legacy application-level singleton used by various legacy components.
 *
 * Even though the actual Android [Application] class is [ShibaMusicApplication], this class continues
 * to exist so existing utility code that relies on static accessors keeps working. The
 * [initializeFromShibaMusicApplication] method lets the real application provide the required context.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val themePref = sharedPreferences.getString(Preferences.THEME, ThemeHelper.DEFAULT_MODE)
        ThemeHelper.applyTheme(themePref ?: ThemeHelper.DEFAULT_MODE)

        instance = this
        context = applicationContext
        Companion.preferences = sharedPreferences
    }

    val preferences: SharedPreferences
        get() {
            if (Companion.preferences == null) {
                Companion.preferences = PreferenceManager.getDefaultSharedPreferences(
                    context ?: applicationContext
                )
            }
            return Companion.preferences!!
        }

    companion object {
        private var instance: App? = null
        private var context: Context? = null
        private var subsonic: Subsonic? = null
        private var github: Github? = null
        private var preferences: SharedPreferences? = null

        /**
         * Initialize the legacy [App] singleton from the modern [ShibaMusicApplication].
         */
        fun initializeFromShibaMusicApplication(application: Application) {
            instance = App()
            context = application.applicationContext
            preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        }

        fun getInstance(): App {
            if (instance == null) {
                instance = App()
            }
            return instance!!
        }

        fun getContext(): Context {
            context?.let { return it }

            instance?.applicationContext?.let {
                context = it
                return it
            }

            throw IllegalStateException("Context not initialized")
        }

        fun getSubsonicClientInstance(override: Boolean): Subsonic {
            if (subsonic == null || override) {
                subsonic = getSubsonicClient()
            }
            return subsonic!!
        }

        fun getGithubClientInstance(): Github {
            if (github == null) {
                github = Github()
            }
            return github!!
        }

        fun refreshSubsonicClient() {
            subsonic = getSubsonicClient()
        }

        private fun getSubsonicClient(): Subsonic {
            val preferences = getSubsonicPreferences()

            preferences.authentication?.let { authentication ->
                authentication.password?.let { Preferences.setPassword(it) }
                authentication.token?.let { Preferences.setToken(it) }
                authentication.salt?.let { Preferences.setSalt(it) }
            }

            return Subsonic(preferences)
        }

        private fun getSubsonicPreferences(): SubsonicPreferences {
            val server = Preferences.getInUseServerAddress()
            val username = Preferences.getUser()
            val password = Preferences.getPassword()
            val token = Preferences.getToken()
            val salt = Preferences.getSalt()
            val isLowSecurity = Preferences.isLowSecurity()

            return SubsonicPreferences().apply {
                serverUrl = server
                this.username = username
                setAuthentication(password, token, salt, isLowSecurity)
            }
        }

        internal fun requirePreferences(): SharedPreferences {
            if (preferences == null) {
                preferences = PreferenceManager.getDefaultSharedPreferences(getContext())
            }
            return preferences!!
        }
    }
}
