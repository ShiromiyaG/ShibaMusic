package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.PodcastEpisodeBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PodcastEpisodeBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var podcastEpisodeBottomSheetViewModel: PodcastEpisodeBottomSheetViewModel
    private lateinit var podcastEpisode: PodcastEpisode

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_podcast_episode_dialog, container, false)

        val args = requireArguments()
        podcastEpisode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(args, Constants.PODCAST_OBJECT, PodcastEpisode::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(Constants.PODCAST_OBJECT)
        } ?: throw IllegalStateException("PodcastEpisode object not found in arguments.")

        podcastEpisodeBottomSheetViewModel = ViewModelProvider(requireActivity())[PodcastEpisodeBottomSheetViewModel::class.java]
        podcastEpisodeBottomSheetViewModel.podcastEpisode = podcastEpisode

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
        val episode = podcastEpisodeBottomSheetViewModel.podcastEpisode ?: run {
            dismissBottomSheet()
            return
        }

        view.apply {
            findViewById<ImageView>(R.id.podcast_cover_image_view).also { coverPodcast ->
                CustomGlideRequest.Builder
                    .from(requireContext(), episode.coverArtId, CustomGlideRequest.ResourceType.Podcast)
                    .build()
                    .into(coverPodcast)
            }

            findViewById<TextView>(R.id.podcast_title_text_view).apply {
                text = episode.title
                isSelected = true
            }

            findViewById<TextView>(R.id.play_next_text_view).setOnClickListener {
                // TODO
                // MediaManager.enqueue(mediaBrowserListenableFuture, podcast, true);
                (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                dismissBottomSheet()
            }

            findViewById<TextView>(R.id.add_to_queue_text_view).setOnClickListener {
                // TODO
                // MediaManager.enqueue(mediaBrowserListenableFuture, podcast, false);
                (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                dismissBottomSheet()
            }

            val download = findViewById<TextView>(R.id.download_text_view)
            download.setOnClickListener {
                // TODO
                /* DownloadUtil.getDownloadTracker(requireContext()).download(
                        MappingUtil.mapMediaItem(podcast, false),
                        MappingUtil.mapDownload(podcast, null, null)
                ); */
                dismissBottomSheet()
            }

            val remove = findViewById<TextView>(R.id.remove_text_view)
            remove.setOnClickListener {
                // TODO
                /* DownloadUtil.getDownloadTracker(requireContext()).remove(
                        MappingUtil.mapMediaItem(podcast, false),
                        MappingUtil.mapDownload(podcast, null, null)
                ); */
                dismissBottomSheet()
            }

            initDownloadUI(download, remove)

            findViewById<TextView>(R.id.delete_text_view).setOnClickListener {
                podcastEpisodeBottomSheetViewModel.deletePodcastEpisode()
                dismissBottomSheet()
            }

            findViewById<TextView>(R.id.go_to_channel_text_view).setOnClickListener {
                Toast.makeText(requireContext(), "Open the channel", Toast.LENGTH_SHORT).show()
                dismissBottomSheet()
            }
        }
    }

    override fun onClick(v: View) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun initDownloadUI(download: TextView, remove: TextView) {
        // TODO
        /* if (DownloadUtil.getDownloadTracker(requireContext()).isDownloaded(MappingUtil.mapMediaItem(podcast, false))) {
            download.visibility = View.GONE
            remove.visibility = View.VISIBLE
        } else {
            download.visibility = View.VISIBLE
            remove.visibility = View.GONE
        } */
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
