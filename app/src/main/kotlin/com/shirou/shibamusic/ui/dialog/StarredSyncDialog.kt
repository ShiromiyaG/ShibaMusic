package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.annotation.NonNull
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogStarredSyncBinding
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.StarredSyncViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class StarredSyncDialog(private val onCancel: Runnable?) : DialogFragment() {
    private lateinit var starredSyncViewModel: StarredSyncViewModel

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogStarredSyncBinding.inflate(layoutInflater)

        starredSyncViewModel = ViewModelProvider(requireActivity())[StarredSyncViewModel::class.java]

        return MaterialAlertDialogBuilder(requireActivity()).apply {
            setView(bind.root)
            setTitle(R.string.starred_sync_dialog_title)
            setPositiveButton(R.string.starred_sync_dialog_positive_button, null)
            setNeutralButton(R.string.starred_sync_dialog_neutral_button, null)
            setNegativeButton(R.string.starred_sync_dialog_negative_button, null)
        }.create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction(requireContext())
    }

    private fun setButtonAction(context: Context) {
        val dialog = dialog as? androidx.appcompat.app.AlertDialog

        dialog?.let { actualDialog ->
            val positiveButton: Button = actualDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                starredSyncViewModel.getStarredTracks(requireActivity()).observe(requireActivity()) { songs ->
                    songs?.let { actualSongs ->
                        DownloadUtil.getDownloadTracker(context).download(
                            MappingUtil.mapDownloads(actualSongs),
                            actualSongs.map { Download(it) }
                        )
                    }
                    actualDialog.dismiss()
                }
            }

            val neutralButton: Button = actualDialog.getButton(Dialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                Preferences.setStarredSyncEnabled(true)
                actualDialog.dismiss()
            }

            val negativeButton: Button = actualDialog.getButton(Dialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                Preferences.setStarredSyncEnabled(false)
                onCancel?.run()
                actualDialog.dismiss()
            }
        }
    }
}
