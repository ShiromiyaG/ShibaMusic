package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle

import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogConnectionAlertBinding
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConnectionAlertDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogConnectionAlertBinding.inflate(layoutInflater)

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setTitle(R.string.connection_alert_dialog_title)
            .setPositiveButton(R.string.connection_alert_dialog_positive_button) { dialog, _ -> dialog.cancel() }
            .setNegativeButton(R.string.connection_alert_dialog_negative_button) { dialog, _ -> dialog.cancel() }

        if (!Preferences.isDataSavingMode()) {
            builder.setNeutralButton(R.string.connection_alert_dialog_neutral_button) { _, _ ->
                // No action needed for neutral button click, action is set in onStart
            }
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        setButtonAction()
    }

    private fun setButtonAction() {
        val alertDialog = dialog as? AlertDialog ?: return // dialog is nullable, cast as AlertDialog, if null, return

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            Preferences.setDataSavingMode(true)
            alertDialog.dismiss()
        }
    }
}
