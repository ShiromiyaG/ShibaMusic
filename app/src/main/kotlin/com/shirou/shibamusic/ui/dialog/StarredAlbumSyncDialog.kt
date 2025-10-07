package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogStarredAlbumSyncBinding
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.StarredAlbumsSyncViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@OptIn(UnstableApi::class)
class StarredAlbumSyncDialog(private val onCancel: (() -> Unit)?) : DialogFragment() {
    private lateinit var starredAlbumsSyncViewModel: StarredAlbumsSyncViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogStarredAlbumSyncBinding.inflate(layoutInflater)

        starredAlbumsSyncViewModel = ViewModelProvider(requireActivity())[StarredAlbumsSyncViewModel::class.java]

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind.root)
            .setTitle(R.string.starred_album_sync_dialog_title)
            .setPositiveButton(R.string.starred_sync_dialog_positive_button, null)
            .setNeutralButton(R.string.starred_sync_dialog_neutral_button, null)
            .setNegativeButton(R.string.starred_sync_dialog_negative_button, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        setButtonAction(requireContext())
    }

    private fun setButtonAction(context: Context) {
        (dialog as? androidx.appcompat.app.AlertDialog)?.let { alertDialog ->
            alertDialog.getButton(Dialog.BUTTON_POSITIVE)?.setOnClickListener {
                starredAlbumsSyncViewModel.getStarredAlbumSongs(requireActivity()).observe(viewLifecycleOwner) { allSongs ->
                    if (allSongs.isNotEmpty()) {
                        DownloadUtil.getDownloadTracker(context).download(
                            MappingUtil.mapDownloads(allSongs),
                            allSongs.map { Download(it) }
                        )
                    }
                    alertDialog.dismiss()
                }
            }

            alertDialog.getButton(Dialog.BUTTON_NEUTRAL)?.setOnClickListener {
                Preferences.setStarredAlbumsSyncEnabled(true)
                alertDialog.dismiss()
            }

            alertDialog.getButton(Dialog.BUTTON_NEGATIVE)?.setOnClickListener {
                Preferences.setStarredAlbumsSyncEnabled(false)
                onCancel?.invoke()
                alertDialog.dismiss()
            }
        }
    }
}
