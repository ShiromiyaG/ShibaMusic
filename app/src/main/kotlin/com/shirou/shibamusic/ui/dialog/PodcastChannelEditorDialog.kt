package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogPodcastChannelEditorBinding
import com.shirou.shibamusic.interfaces.PodcastCallback
import com.shirou.shibamusic.viewmodel.PodcastChannelEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PodcastChannelEditorDialog(
    private val podcastCallback: PodcastCallback
) : DialogFragment() {

    private var bind: DialogPodcastChannelEditorBinding? = null
    private lateinit var podcastChannelEditorViewModel: PodcastChannelEditorViewModel

    private var channelUrl: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogPodcastChannelEditorBinding.inflate(layoutInflater)

        podcastChannelEditorViewModel = ViewModelProvider(requireActivity()).get(PodcastChannelEditorViewModel::class.java)

        return MaterialAlertDialogBuilder(requireContext())
            .apply {
                setView(bind!!.root)
                setTitle(R.string.podcast_channel_editor_dialog_title)
                setPositiveButton(R.string.radio_editor_dialog_positive_button) { _, _ -> }
                setNegativeButton(R.string.radio_editor_dialog_negative_button) { dialog, _ ->
                    dialog.cancel()
                }
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setButtonAction() {
        (dialog as? AlertDialog)?.let { alertDialog ->
            val positiveButton: Button = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateInput()) {
                    podcastChannelEditorViewModel.createChannel(channelUrl)
                    dismissDialog()
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        channelUrl = bind!!.podcastChannelRssUrlNameTextView.text.toString().trim()

        if (channelUrl.isEmpty()) {
            bind!!.podcastChannelRssUrlNameTextView.error = getString(R.string.error_required)
            return false
        }

        return true
    }

    private fun dismissDialog() {
        podcastCallback.onDismiss()
        dialog!!.dismiss()
    }
}
