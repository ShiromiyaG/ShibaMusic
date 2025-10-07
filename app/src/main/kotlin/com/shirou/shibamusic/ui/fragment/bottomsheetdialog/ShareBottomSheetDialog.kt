package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.ui.dialog.ShareUpdateDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.HomeViewModel
import com.shirou.shibamusic.viewmodel.ShareBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

@UnstableApi
class ShareBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var shareBottomSheetViewModel: ShareBottomSheetViewModel
    private var share: Share? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_share_dialog, container, false)

        share = BundleCompat.getParcelable(requireArguments(), Constants.SHARE_OBJECT, Share::class.java)

        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        shareBottomSheetViewModel = ViewModelProvider(requireActivity())[ShareBottomSheetViewModel::class.java].apply {
            this.share = this@ShareBottomSheetDialog.share
        }

        share?.let {
            init(view, it)
        } ?: run {
            dismissAllowingStateLoss()
        }

        return view
    }

    private fun init(view: View, share: Share) {
        val shareCover: ImageView = view.findViewById(R.id.share_cover_image_view)
        val coverArtId = share.entries?.firstOrNull()?.coverArtId
        CustomGlideRequest.Builder
            .from(requireContext(), coverArtId, CustomGlideRequest.ResourceType.Unknown)
            .build()
            .into(shareCover)

        view.findViewById<TextView>(R.id.share_title_text_view).apply {
            text = share.description.orEmpty()
            isSelected = true
        }

        view.findViewById<TextView>(R.id.share_subtitle_text_view).apply {
            val expiration = share.expires?.let { UIUtil.getReadableDate(it) } ?: "-"
            text = requireContext().getString(R.string.share_subtitle_item, expiration)
            isSelected = true
        }

        view.findViewById<TextView>(R.id.copy_link_text_view).setOnClickListener {
            share.url?.let { url ->
                val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText(getString(R.string.app_name), url)
                clipboardManager.setPrimaryClip(clipData)
            }
            dismissBottomSheet()
        }

        view.findViewById<TextView>(R.id.update_share_preferences_text_view).setOnClickListener {
            showUpdateShareDialog()
            dismissBottomSheet()
        }

        view.findViewById<TextView>(R.id.delete_share_text_view).setOnClickListener {
            deleteShare()
            refreshShares()
            dismissBottomSheet()
        }
    }

    override fun onClick(v: View) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun showUpdateShareDialog() {
        val dialog = ShareUpdateDialog()
        dialog.show(requireActivity().supportFragmentManager, null)
    }

    private fun refreshShares() {
        val owner: LifecycleOwner = parentFragment?.viewLifecycleOwner ?: run {
            val activity = activity
            if (activity is LifecycleOwner) {
                activity
            } else {
                return
            }
        }

        homeViewModel.refreshShares(owner)
    }

    private fun deleteShare() {
        shareBottomSheetViewModel.deleteShare()
    }
}
