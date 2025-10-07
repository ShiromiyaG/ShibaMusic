package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogDownloadStorageBinding
import com.shirou.shibamusic.interfaces.DialogClickCallback
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class DownloadStorageDialog(private val dialogClickCallback: DialogClickCallback) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogDownloadStorageBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind.root)
            .setTitle(R.string.download_storage_dialog_title)
            .setPositiveButton(R.string.download_storage_external_dialog_positive_button, null)
            .setNegativeButton(R.string.download_storage_internal_dialog_negative_button, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction()
    }

    private fun setButtonAction() {
        (dialog as? androidx.appcompat.app.AlertDialog)?.let { alertDialog ->
            val positiveButton: Button = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPreference = Preferences.getDownloadStoragePreference()
                val newPreference = 1

                if (currentPreference != newPreference) {
                    Preferences.setDownloadStoragePreference(newPreference)
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                    dialogClickCallback.onPositiveClick()
                }
                alertDialog.dismiss()
            }

            val negativeButton: Button = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                val currentPreference = Preferences.getDownloadStoragePreference()
                val newPreference = 0

                if (currentPreference != newPreference) {
                    Preferences.setDownloadStoragePreference(newPreference)
                    DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                    dialogClickCallback.onNegativeClick()
                }
                alertDialog.dismiss()
            }
        }
    }
}
