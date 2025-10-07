package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.InnerFragmentPlayerCoverBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.dialog.PlaylistChooserDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.PlayerBottomSheetViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.ArrayList

@UnstableApi
class PlayerCoverFragment : Fragment() {
    private lateinit var playerBottomSheetViewModel: PlayerBottomSheetViewModel
    private var bind: InnerFragmentPlayerCoverBinding? = null
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bind = InnerFragmentPlayerCoverBinding.inflate(inflater, container, false)
        val view = bind!!.root

        playerBottomSheetViewModel = ViewModelProvider(requireActivity())[PlayerBottomSheetViewModel::class.java]

        initOverlay()
        initInnerButton()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
        bindMediaController()
        toggleOverlayVisibility(false)
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        bind = null
    }

    private fun initTapButtonHideTransition() {
        bind?.nowPlayingTapButton?.visibility = View.VISIBLE

        handler.removeCallbacksAndMessages(null)

        val runnable = Runnable {
            bind?.nowPlayingTapButton?.visibility = View.GONE
        }

        handler.postDelayed(runnable, 10000)
    }

    private fun initOverlay() {
        bind?.nowPlayingSongCoverImageView?.setOnClickListener { toggleOverlayVisibility(true) }
        bind?.nowPlayingSongCoverButtonGroup?.setOnClickListener { toggleOverlayVisibility(false) }
        bind?.nowPlayingTapButton?.setOnClickListener { toggleOverlayVisibility(true) }
    }

    private fun toggleOverlayVisibility(isVisible: Boolean) {
        val transition: Transition = Fade().apply { duration = 200 }
        bind?.nowPlayingSongCoverButtonGroup?.let { transition.addTarget(it) }

        bind?.root?.let { TransitionManager.beginDelayedTransition(it, transition) }
    bind?.nowPlayingSongCoverButtonGroup?.visibility = if (isVisible) View.VISIBLE else View.GONE
    bind?.nowPlayingTapButton?.visibility = if (isVisible) View.GONE else View.VISIBLE

    val isQueueSyncEnabled = Preferences.isSynchronizationEnabled()
    bind?.innerButtonBottomRight?.visibility = if (isQueueSyncEnabled) View.VISIBLE else View.GONE
    bind?.innerButtonBottomRightAlternative?.visibility = if (isQueueSyncEnabled) View.GONE else View.VISIBLE

        if (!isVisible) initTapButtonHideTransition()
    }

    private fun initInnerButton() {
        playerBottomSheetViewModel.liveMedia.observe(viewLifecycleOwner) { song ->
            song?.let { media ->
                bind?.let { binding ->
                    binding.innerButtonTopLeft.setOnClickListener {
                        DownloadUtil.getDownloadTracker(requireContext()).download(
                            MappingUtil.mapDownload(media),
                            Download(media)
                        )
                    }

                    binding.innerButtonTopRight.setOnClickListener {
                        val tracks = ArrayList<Child>().apply { add(media) }
                        val bundle = Bundle().apply { putParcelableArrayList(Constants.TRACKS_OBJECT, tracks) }

                        PlaylistChooserDialog().apply {
                            arguments = bundle
                        }.show(requireActivity().supportFragmentManager, null)
                    }

                    binding.innerButtonBottomLeft.setOnClickListener {
                        playerBottomSheetViewModel.getMediaInstantMix(viewLifecycleOwner, media).observe(viewLifecycleOwner) { instantMixMedia ->
                            instantMixMedia?.let {
                                MediaManager.enqueue(mediaBrowserListenableFuture, it, true)
                            }
                        }
                    }

                    binding.innerButtonBottomRight.setOnClickListener {
                        if (playerBottomSheetViewModel.savePlayQueue()) {
                            Snackbar.make(requireView(), R.string.player_queue_save_queue_success, Snackbar.LENGTH_LONG).show()
                        }
                    }

                    binding.innerButtonBottomRightAlternative.setOnClickListener {
                        activity?.let { activity ->
                            val playerBottomSheetFragment = activity.supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment
                            playerBottomSheetFragment?.goToLyricsPage()
                        }
                    }
                }
            }
        }
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(requireContext(), SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))).buildAsync()
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture.addListener({
            try {
                val mediaBrowser = mediaBrowserListenableFuture.get()
                setMediaBrowserListener(mediaBrowser)
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaBrowserListener(mediaBrowser: MediaBrowser) {
        setCover(mediaBrowser.mediaMetadata)

        mediaBrowser.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                setCover(mediaMetadata)
                toggleOverlayVisibility(false)
            }
        })
    }

    private fun setCover(mediaMetadata: MediaMetadata) {
        bind?.nowPlayingSongCoverImageView?.let { imageView ->
            CustomGlideRequest.Builder
                .from(requireContext(), mediaMetadata.extras?.getString("coverArtId"), CustomGlideRequest.ResourceType.Song)
                .build()
                .into(imageView)
        }
    }
}
