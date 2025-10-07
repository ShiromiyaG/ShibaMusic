package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogDownloadDirectoryBinding
import com.shirou.shibamusic.interfaces.DialogClickCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class DownloadDirectoryDialog(
    private val dialogClickCallback: DialogClickCallback
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDownloadDirectoryBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext()).apply {
            setView(binding.root)
            setTitle(R.string.download_directory_dialog_title)
            setPositiveButton(R.string.download_directory_dialog_positive_button, null)
            setNegativeButton(R.string.download_directory_dialog_negative_button, null)
        }.create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction()
    }

    private fun setButtonAction() {
        (dialog as? androidx.appcompat.app.AlertDialog)?.let { alertDialog ->
            val positiveButton: Button = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                dialogClickCallback.onPositiveClick()
                alertDialog.dismiss()
            }

            val negativeButton: Button = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                dialogClickCallback.onNegativeClick()
                alertDialog.dismiss()
            }
        }
    }
}
