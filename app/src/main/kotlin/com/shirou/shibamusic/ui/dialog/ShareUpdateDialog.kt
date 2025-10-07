package com.shirou.shibamusic.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogShareUpdateBinding
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.HomeViewModel
import com.shirou.shibamusic.viewmodel.ShareBottomSheetViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date

class ShareUpdateDialog : DialogFragment() {
    private var bind: DialogShareUpdateBinding? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var shareBottomSheetViewModel: ShareBottomSheetViewModel

    private var datePicker: MaterialDatePicker<Long>? = null

    private lateinit var descriptionTextView: String
    private lateinit var expirationTextView: String
    private var expiration: Long = 0L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        shareBottomSheetViewModel = ViewModelProvider(requireActivity())[ShareBottomSheetViewModel::class.java]

        bind = DialogShareUpdateBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .apply {
                setView(bind!!.root)
                setTitle(R.string.share_update_dialog_title)
                setPositiveButton(R.string.share_update_dialog_positive_button) { _, _ -> }
                setNegativeButton(R.string.share_update_dialog_negative_button) { dialog, _ -> dialog.cancel() }
            }
            .create()
    }

    override fun onStart() {
        super.onStart()

        setShareInfo()
        setShareCalendar()
        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setShareInfo() {
        shareBottomSheetViewModel.share?.let { share ->
            bind?.shareDescriptionTextView?.setText(share.description.orEmpty())
            // bind?.shareExpirationTextView?.setText(share.expires)
        }
    }

    private fun setShareCalendar() {
        expiration = shareBottomSheetViewModel.share?.expires?.time ?: 0L

    bind?.shareExpirationTextView?.setText(UIUtil.getReadableDate(Date(expiration)))

        bind?.shareExpirationTextView?.isFocusable = false
        bind?.shareExpirationTextView?.setOnLongClickListener(null)

        bind?.shareExpirationTextView?.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .apply { setValidator(DateValidatorPointForward.now()) }
                .build()

            datePicker = MaterialDatePicker.Builder.datePicker()
                .apply {
                    setCalendarConstraints(constraints)
                    setSelection(expiration)
                }
                .build()

            datePicker?.addOnPositiveButtonClickListener { selection ->
                expiration = selection
                bind?.shareExpirationTextView?.setText(UIUtil.getReadableDate(Date(selection)))
            }

            datePicker?.show(requireActivity().supportFragmentManager, null)
        }
    }

    private fun setButtonAction() {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateInput()) {
                updateShare()
                dialog?.dismiss()
            }
        }
    }

    private fun validateInput(): Boolean {
        descriptionTextView = bind!!.shareDescriptionTextView.text?.toString()?.trim() ?: ""
        expirationTextView = bind!!.shareExpirationTextView.text?.toString()?.trim() ?: ""

        if (descriptionTextView.isEmpty()) {
            bind!!.shareDescriptionTextView.error = getString(R.string.error_required)
            return false
        }

        if (expirationTextView.isEmpty()) {
            bind!!.shareExpirationTextView.error = getString(R.string.error_required)
            return false
        }

        return true
    }

    private fun updateShare() {
        shareBottomSheetViewModel.updateShare(descriptionTextView, expiration)
        homeViewModel.refreshShares(requireActivity())
    }
}
