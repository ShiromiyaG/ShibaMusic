package com.shirou.shibamusic.ui.fragment

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.R
import com.shirou.shibamusic.helper.ThemeHelper
import com.shirou.shibamusic.interfaces.DialogClickCallback
import com.shirou.shibamusic.interfaces.ScanCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.dialog.DeleteDownloadStorageDialog
import com.shirou.shibamusic.ui.dialog.DownloadStorageDialog
import com.shirou.shibamusic.ui.dialog.StarredAlbumSyncDialog
import com.shirou.shibamusic.ui.dialog.StarredSyncDialog
import com.shirou.shibamusic.ui.dialog.StreamingCacheStorageDialog
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.SettingViewModel
import java.util.Locale

@OptIn(UnstableApi::class)
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var activity: MainActivity
    private lateinit var settingViewModel: SettingViewModel
    private lateinit var someActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        someActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // No action needed for the result in the original Java code
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        val view = super.onCreateView(inflater, container, savedInstanceState)
        settingViewModel = ViewModelProvider(requireActivity())[SettingViewModel::class.java]

        listView.setPadding(0, 0, 0, resources.getDimension(R.dimen.global_padding_bottom).toInt())

        return view ?: throw IllegalStateException("PreferenceFragmentCompat returned null root view")
    }

    override fun onStart() {
        super.onStart()
        activity.setBottomNavigationBarVisibility(false)
        activity.setBottomSheetVisibility(false)
    }

    override fun onResume() {
        super.onResume()

        checkEqualizer()
        checkCacheStorage()
        checkStorage()

        setStreamingCacheSize()
        setAppLanguage()
        setVersion()

        actionLogout()
        actionScan()
        actionSyncStarredAlbums()
        actionSyncStarredTracks()
        actionChangeStreamingCacheStorage()
        actionChangeDownloadStorage()
        actionDeleteDownloadStorage()
        actionKeepScreenOn()
    }

    override fun onStop() {
        super.onStop()
        activity.setBottomSheetVisibility(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey)
        val themePreference: ListPreference? = findPreference(Preferences.THEME)
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeOption = newValue as? String ?: return@setOnPreferenceChangeListener false
            ThemeHelper.applyTheme(themeOption)
            true
        }
    }

    private fun checkEqualizer() {
        val equalizer: Preference? = findPreference("equalizer")
        equalizer ?: return

        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            equalizer.setOnPreferenceClickListener {
                someActivityResultLauncher.launch(intent)
                true
            }
        } else {
            equalizer.isVisible = false
        }
    }

    private fun checkCacheStorage() {
        val storage: Preference? = findPreference("streaming_cache_storage")
        storage ?: return

        try {
            if (requireContext().getExternalFilesDirs(null).getOrNull(1) == null) {
                storage.isVisible = false
            } else {
                storage.summary = if (Preferences.getDownloadStoragePreference() == 0) {
                    getString(R.string.download_storage_internal_dialog_negative_button)
                } else {
                    getString(R.string.download_storage_external_dialog_positive_button)
                }
            }
        } catch (exception: Exception) {
            storage.isVisible = false
        }
    }

    private fun checkStorage() {
        val storage: Preference? = findPreference("download_storage")
        storage ?: return

        try {
            if (requireContext().getExternalFilesDirs(null).getOrNull(1) == null) {
                storage.isVisible = false
            } else {
                storage.summary = if (Preferences.getDownloadStoragePreference() == 0) {
                    getString(R.string.download_storage_internal_dialog_negative_button)
                } else {
                    getString(R.string.download_storage_external_dialog_positive_button)
                }
            }
        } catch (exception: Exception) {
            storage.isVisible = false
        }
    }

    private fun setStreamingCacheSize() {
        val streamingCachePreference: ListPreference? = findPreference("streaming_cache_size")

        streamingCachePreference?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
            val entry = preference.entry ?: return@SummaryProvider null
            val currentSizeMb = DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024L * 1024L)
            getString(R.string.settings_summary_streaming_cache_size, entry, currentSizeMb.toString())
        }
    }

    private fun setAppLanguage() {
        val localePref: ListPreference = findPreference("language") ?: return

        val locales = UIUtil.getLangPreferenceDropdownEntries(requireContext())

        val entries: Array<CharSequence> = locales.map { it.key as CharSequence }.toTypedArray()
        val entryValues: Array<CharSequence> = locales.map { it.value as CharSequence }.toTypedArray()

        localePref.entries = entries
        localePref.entryValues = entryValues

        val value = localePref.value
        if (value == "default") {
            localePref.summary = requireContext().getString(R.string.settings_system_language)
        } else {
            localePref.summary = Locale.forLanguageTag(value).displayName
        }

        localePref.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue == "default") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                preference.summary = requireContext().getString(R.string.settings_system_language)
            } else {
                val appLocale = LocaleListCompat.forLanguageTags(newValue as String)
                AppCompatDelegate.setApplicationLocales(appLocale)
                preference.summary = Locale.forLanguageTag(newValue).displayName
            }
            true
        }
    }

    private fun setVersion() {
        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME
    }

    private fun actionLogout() {
        findPreference<Preference>("logout")?.setOnPreferenceClickListener {
            activity.quit()
            true
        }
    }

    private fun actionScan() {
        findPreference<Preference>("scan_library")?.setOnPreferenceClickListener {
            settingViewModel.launchScan(object : ScanCallback {
                override fun onError(exception: Exception) {
                    findPreference<Preference>("scan_library")?.summary = exception.message
                }

                override fun onSuccess(isScanning: Boolean, count: Long) {
                    findPreference<Preference>("scan_library")?.summary = "Scanning: counting $count tracks"
                    if (isScanning) getScanStatus()
                }
            })
            true
        }
    }

    private fun actionSyncStarredTracks() {
        findPreference<SwitchPreference>("sync_starred_tracks_for_offline_use")?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean) {
                if (newValue) {
                    val dialog = StarredSyncDialog {
                        (preference as SwitchPreference).isChecked = false
                    }
                    dialog.show(activity.supportFragmentManager, null)
                }
            }
            true
        }
    }

    private fun actionSyncStarredAlbums() {
        findPreference<SwitchPreference>("sync_starred_albums_for_offline_use")?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean) {
                if (newValue) {
                    val dialog = StarredAlbumSyncDialog {
                        (preference as SwitchPreference).isChecked = false
                    }
                    dialog.show(activity.supportFragmentManager, null)
                }
            }
            true
        }
    }

    private fun actionChangeStreamingCacheStorage() {
        findPreference<Preference>("streaming_cache_storage")?.setOnPreferenceClickListener {
            val dialog = StreamingCacheStorageDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    findPreference<Preference>("streaming_cache_storage")?.summary =
                        getString(R.string.streaming_cache_storage_external_dialog_positive_button)
                }

                override fun onNegativeClick() {
                    findPreference<Preference>("streaming_cache_storage")?.summary =
                        getString(R.string.streaming_cache_storage_internal_dialog_negative_button)
                }
            })
            dialog.show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun actionChangeDownloadStorage() {
        findPreference<Preference>("download_storage")?.setOnPreferenceClickListener {
            val dialog = DownloadStorageDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    findPreference<Preference>("download_storage")?.summary =
                        getString(R.string.download_storage_external_dialog_positive_button)
                }

                override fun onNegativeClick() {
                    findPreference<Preference>("download_storage")?.summary =
                        getString(R.string.download_storage_internal_dialog_negative_button)
                }
            })
            dialog.show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun actionDeleteDownloadStorage() {
        findPreference<Preference>("delete_download_storage")?.setOnPreferenceClickListener {
            val dialog = DeleteDownloadStorageDialog()
            dialog.show(activity.supportFragmentManager, null)
            true
        }
    }

    private fun getScanStatus() {
        settingViewModel.getScanStatus(object : ScanCallback {
            override fun onError(exception: Exception) {
                findPreference<Preference>("scan_library")?.summary = exception.message
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                findPreference<Preference>("scan_library")?.summary = "Scanning: counting $count tracks"
                if (isScanning) getScanStatus()
            }
        })
    }

    private fun actionKeepScreenOn() {
        findPreference<Preference>("always_on_display")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (newValue) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            true
        }
    }
}
