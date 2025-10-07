package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.PodcastChannelBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PodcastChannelBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var podcastChannelBottomSheetViewModel: PodcastChannelBottomSheetViewModel
    private lateinit var podcastChannel: PodcastChannel

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_podcast_channel_dialog, container, false)

        val args = requireArguments()
        podcastChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(args, Constants.PODCAST_CHANNEL_OBJECT, PodcastChannel::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(Constants.PODCAST_CHANNEL_OBJECT)
        } ?: throw IllegalStateException("PodcastChannelBottomSheetDialog requires a PodcastChannel argument")

        podcastChannelBottomSheetViewModel = ViewModelProvider(requireActivity())[PodcastChannelBottomSheetViewModel::class.java].apply {
            this.podcastChannel = this@PodcastChannelBottomSheetDialog.podcastChannel
        }

        init(view)

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    private fun init(view: View) {
        val channel = podcastChannelBottomSheetViewModel.podcastChannel ?: return

        val coverPodcast: ImageView = view.findViewById(R.id.podcast_cover_image_view)

        CustomGlideRequest.Builder
            .from(requireContext(), channel.coverArtId, CustomGlideRequest.ResourceType.Podcast)
            .build()
            .into(coverPodcast)

        val titlePodcast: TextView = view.findViewById(R.id.podcast_title_text_view)
        titlePodcast.text = channel.title

        val delete: TextView = view.findViewById(R.id.delete_text_view)
        delete.setOnClickListener {
            podcastChannelBottomSheetViewModel.deletePodcastChannel()
            dismissBottomSheet()
        }
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        mediaBrowserListenableFuture?.let { MediaBrowser.releaseFuture(it) }
        mediaBrowserListenableFuture = null
    }
}
