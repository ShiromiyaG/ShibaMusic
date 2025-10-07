package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle

import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogServerUnreachableBinding
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class ServerUnreachableDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogServerUnreachableBinding.inflate(layoutInflater)

        val popup = MaterialAlertDialogBuilder(requireActivity()).apply {
            setView(bind.root)
            setTitle(R.string.server_unreachable_dialog_title)
            setPositiveButton(R.string.server_unreachable_dialog_positive_button, null)
            setNeutralButton(R.string.server_unreachable_dialog_neutral_button, null)
            setNegativeButton(R.string.server_unreachable_dialog_negative_button) { dialog, _ -> dialog.cancel() }
        }.create()

        popup.setCanceledOnTouchOutside(false)
        popup.setCancelable(false)

        return popup
    }

    override fun onStart() {
        super.onStart()
        setButtonAction()
    }

    private fun setButtonAction() {
        // In onStart, 'dialog' property is guaranteed to be non-null and refers to the Dialog returned by onCreateDialog.
        // Since onCreateDialog created an AlertDialog, we can safely cast it.
        val alertDialog = dialog as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            // 'activity' property is nullable, so use safe cast and safe call
            (activity as? MainActivity)?.quit()
            alertDialog.dismiss()
        }

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            Preferences.setServerUnreachableDatetime()
            alertDialog.dismiss()
        }
    }
}
