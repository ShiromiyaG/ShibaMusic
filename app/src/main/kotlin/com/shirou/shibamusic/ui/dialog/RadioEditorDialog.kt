package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogRadioEditorBinding
import com.shirou.shibamusic.interfaces.RadioCallback
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.RadioEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RadioEditorDialog(private val radioCallback: RadioCallback) : DialogFragment() {

    private var bind: DialogRadioEditorBinding? = null
    private lateinit var radioEditorViewModel: RadioEditorViewModel

    private var radioName: String = ""
    private var radioStreamURL: String = ""
    private var radioHomepageURL: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogRadioEditorBinding.inflate(layoutInflater)

        radioEditorViewModel = ViewModelProvider(requireActivity())[RadioEditorViewModel::class.java]

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind?.root)
            .setTitle(R.string.radio_editor_dialog_title)
            .setPositiveButton(R.string.radio_editor_dialog_positive_button) { _, _ ->
                if (validateInput()) {
                    if (radioEditorViewModel.radioToEdit == null) {
                        radioEditorViewModel.createRadio(radioName, radioStreamURL, radioHomepageURL.ifEmpty { null })
                    } else {
                        radioEditorViewModel.updateRadio(radioName, radioStreamURL, radioHomepageURL.ifEmpty { null })
                    }
                    dismissDialog()
                }
            }
            .setNeutralButton(R.string.radio_editor_dialog_neutral_button) { _, _ ->
                radioEditorViewModel.deleteRadio()
                dismissDialog()
            }
            .setNegativeButton(R.string.radio_editor_dialog_negative_button) { dialog, _ ->
                dialog.cancel()
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        setParameterInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setParameterInfo() {
        arguments?.getParcelable<InternetRadioStation>(Constants.INTERNET_RADIO_STATION_OBJECT)?.let { toEdit ->
            radioEditorViewModel.radioToEdit = toEdit
            bind?.apply {
                internetRadioStationNameTextView.setText(toEdit.name.orEmpty())
                internetRadioStationStreamUrlTextView.setText(toEdit.streamUrl.orEmpty())
                internetRadioStationHomepageUrlTextView.setText(toEdit.homePageUrl.orEmpty())
            }
        }
    }

    private fun validateInput(): Boolean {
        bind?.apply {
            radioName = internetRadioStationNameTextView.text.toString().trim()
            radioStreamURL = internetRadioStationStreamUrlTextView.text.toString().trim()
            radioHomepageURL = internetRadioStationHomepageUrlTextView.text.toString().trim()

            if (radioName.isEmpty()) {
                internetRadioStationNameTextView.error = getString(R.string.error_required)
                return false
            }

            if (radioStreamURL.isEmpty()) {
                internetRadioStationStreamUrlTextView.error = getString(R.string.error_required)
                return false
            }
        } ?: return false

        return true
    }

    private fun dismissDialog() {
        radioCallback.onDismiss()
        dialog?.dismiss()
    }
}
