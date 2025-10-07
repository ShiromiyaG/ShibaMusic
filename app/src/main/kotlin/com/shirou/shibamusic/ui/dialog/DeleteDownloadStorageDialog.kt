package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogDeleteDownloadStorageBinding
import com.shirou.shibamusic.util.DownloadUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class DeleteDownloadStorageDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogDeleteDownloadStorageBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind.root)
            .setTitle(R.string.delete_download_storage_dialog_title)
            .setPositiveButton(R.string.delete_download_storage_dialog_positive_button, null)
            .setNegativeButton(R.string.delete_download_storage_dialog_negative_button, null)
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
                DownloadUtil.getDownloadTracker(requireContext()).removeAll()
                actualDialog.dismiss()
            }

            val negativeButton: Button = actualDialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                actualDialog.dismiss()
            }
        }
    }
}
