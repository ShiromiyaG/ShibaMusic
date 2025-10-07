package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogStreamingCacheStorageBinding
import com.shirou.shibamusic.interfaces.DialogClickCallback
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class StreamingCacheStorageDialog(private val dialogClickCallback: DialogClickCallback) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogStreamingCacheStorageBinding.inflate(layoutInflater)

    return MaterialAlertDialogBuilder(requireContext())
                .setView(bind.root)
                .setTitle(R.string.streaming_cache_storage_dialog_title)
                .setPositiveButton(R.string.streaming_cache_storage_external_dialog_positive_button, null)
                .setNegativeButton(R.string.streaming_cache_storage_internal_dialog_negative_button, null)
                .create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction()
    }

    private fun setButtonAction() {
        (dialog as? androidx.appcompat.app.AlertDialog)?.let { actualDialog ->
            val positiveButton: Button = actualDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPreference = Preferences.getStreamingCacheStoragePreference()
                val newPreference = 1

                if (currentPreference != newPreference) {
                    Preferences.setStreamingCacheStoragePreference(newPreference)
                    dialogClickCallback.onPositiveClick()
                }
                actualDialog.dismiss()
            }

            val negativeButton: Button = actualDialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                val currentPreference = Preferences.getStreamingCacheStoragePreference()
                val newPreference = 0

                if (currentPreference != newPreference) {
                    Preferences.setStreamingCacheStoragePreference(newPreference)
                    dialogClickCallback.onNegativeClick()
                }
                actualDialog.dismiss()
            }
        }
    }
}
