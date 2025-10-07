package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogGithubShibamusicUpdateBinding
import com.shirou.shibamusic.github.models.LatestRelease
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GithubShibaMusicUpdateDialog(private val latestRelease: LatestRelease) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogGithubShibamusicUpdateBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind.root)
            .setTitle(R.string.github_update_dialog_title)
            .setPositiveButton(R.string.github_update_dialog_positive_button) { _, _ ->
                // Actions will be set in onStart to allow button customization
            }
            .setNegativeButton(R.string.github_update_dialog_negative_button) { _, _ ->
                // Actions will be set in onStart
            }
            .setNeutralButton(R.string.github_update_dialog_neutral_button) { _, _ ->
                // Actions will be set in onStart
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        setButtonAction()
    }

    private fun setButtonAction() {
        // requireDialog() ensures the dialog is not null.
        // We cast it to AlertDialog as MaterialAlertDialogBuilder.create() returns an AlertDialog.
        val alertDialog = requireDialog() as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            latestRelease.htmlUrl?.let { openLink(it) }
            alertDialog.dismiss()
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            Preferences.setShibaMusicUpdateReminder()
            alertDialog.dismiss()
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            openLink(getString(R.string.support_url))
            alertDialog.dismiss()
        }
    }

    private fun openLink(link: String) {
        Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also {
            startActivity(it)
        }
    }
}
