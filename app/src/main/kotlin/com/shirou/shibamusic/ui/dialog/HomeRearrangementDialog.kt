package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogHomeRearrangementBinding
import com.shirou.shibamusic.ui.adapter.HomeSectorHorizontalAdapter
import com.shirou.shibamusic.viewmodel.HomeRearrangementViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import java.util.Collections

class HomeRearrangementDialog : DialogFragment() {

    private var _binding: DialogHomeRearrangementBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeRearrangementViewModel: HomeRearrangementViewModel
    private lateinit var homeSectorHorizontalAdapter: HomeSectorHorizontalAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogHomeRearrangementBinding.inflate(layoutInflater)

        homeRearrangementViewModel = ViewModelProvider(requireActivity())[HomeRearrangementViewModel::class.java]

        return MaterialAlertDialogBuilder(requireContext()).apply {
            setView(binding.root)
            setTitle(R.string.home_rearrangement_dialog_title)
            setPositiveButton(R.string.home_rearrangement_dialog_positive_button) { _, _ -> }
            setNeutralButton(R.string.home_rearrangement_dialog_neutral_button) { _, _ -> }
            setNegativeButton(R.string.home_rearrangement_dialog_negative_button) { dialog, _ -> dialog.cancel() }
        }.create()
    }

    override fun onStart() {
        super.onStart()
        setButtonAction()
        initSectorView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeRearrangementViewModel.closeDialog()
        _binding = null
    }

    private fun setButtonAction() {
        val alertDialog = requireDialog() as androidx.appcompat.app.AlertDialog

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            homeRearrangementViewModel.saveHomeSectorList(homeSectorHorizontalAdapter.getItems())
            dismiss()
        }

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            homeRearrangementViewModel.resetHomeSectorList()
            dismiss()
        }
    }

    private fun initSectorView() {
        binding.homeSectorItemRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            homeSectorHorizontalAdapter = HomeSectorHorizontalAdapter()
            adapter = homeSectorHorizontalAdapter
        }

        homeSectorHorizontalAdapter.setItems(homeRearrangementViewModel.getHomeSectorList())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            private var originalPosition = -1
            private var fromPosition = -1
            private var toPosition = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1) originalPosition = viewHolder.bindingAdapterPosition

                fromPosition = viewHolder.bindingAdapterPosition
                toPosition = target.bindingAdapterPosition

                Collections.swap(homeSectorHorizontalAdapter.getItems(), fromPosition, toPosition)
                recyclerView.adapter!!.notifyItemMoved(fromPosition, toPosition)

                return false
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                homeRearrangementViewModel.orderSectorLiveListAfterSwap(homeSectorHorizontalAdapter.getItems())

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not implemented in original Java code
            }
        }).attachToRecyclerView(binding.homeSectorItemRecyclerView)
    }
}
