package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentPlayerBottomSheetBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.PlayQueue
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.fragment.pager.PlayerControllerVerticalPager
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.PlayerBottomSheetViewModel
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

@OptIn(UnstableApi::class)
class PlayerBottomSheetFragment : Fragment() {
    private var _binding: FragmentPlayerBottomSheetBinding? = null
    private val binding get() = _binding!!

    val playerHeader: View?
        get() = _binding?.playerHeaderLayout?.root

    private lateinit var playerBottomSheetViewModel: PlayerBottomSheetViewModel
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    private lateinit var progressBarHandler: Handler
    private lateinit var progressBarRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBottomSheetBinding.inflate(inflater, container, false)
        val view = binding.root

        playerBottomSheetViewModel = ViewModelProvider(requireActivity())[PlayerBottomSheetViewModel::class.java]

        customizeBottomSheetBackground()
        customizeBottomSheetAction()
        initViewPager()
        setHeaderBookmarksButton()

        return view
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
        bindMediaController()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun customizeBottomSheetBackground() {
    binding.playerHeaderLayout.root.setBackgroundColor(SurfaceColors.getColorForElevation(requireContext(), 8f))
    }

    private fun customizeBottomSheetAction() {
        binding.playerHeaderLayout.root.setOnClickListener {
            (requireActivity() as MainActivity).expandBottomSheet()
        }
    }

    private fun initViewPager() {
        binding.playerBodyLayout.playerBodyBottomSheetViewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.playerBodyLayout.playerBodyBottomSheetViewPager.adapter = PlayerControllerVerticalPager(this)
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        MediaController.releaseFuture(mediaBrowserListenableFuture)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture.addListener({
            try {
                val mediaBrowser = mediaBrowserListenableFuture.get()

                mediaBrowser.shuffleModeEnabled = Preferences.isShuffleModeEnabled()
                mediaBrowser.repeatMode = Preferences.getRepeatMode()

                setMediaControllerListener(mediaBrowser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaControllerListener(mediaBrowser: MediaBrowser) {
        defineProgressBarHandler(mediaBrowser)
        setMediaControllerUI(mediaBrowser)
        setMetadata(mediaBrowser.mediaMetadata)
        setContentDuration(mediaBrowser.contentDuration)
        setPlayingState(mediaBrowser.isPlaying)
        setHeaderMediaController()
        setHeaderNextButtonState(mediaBrowser.hasNextMediaItem())

        mediaBrowser.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(@NonNull mediaMetadata: MediaMetadata) {
                setMediaControllerUI(mediaBrowser)
                setMetadata(mediaMetadata)
                setContentDuration(mediaBrowser.contentDuration)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                setPlayingState(isPlaying)
            }

            override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
                super.onSkipSilenceEnabledChanged(skipSilenceEnabled)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                setHeaderNextButtonState(mediaBrowser.hasNextMediaItem())
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                Preferences.setShuffleModeEnabled(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Preferences.setRepeatMode(repeatMode)
            }
        })
    }

    private fun setMetadata(mediaMetadata: MediaMetadata) {
        mediaMetadata.extras?.let { extras ->
            val mediaType = extras.getString("type")

            extras.getString("id")?.let { mediaId ->
                playerBottomSheetViewModel.setLiveMedia(
                    viewLifecycleOwner,
                    mediaType,
                    mediaId
                )
            }

            extras.getString("albumId")?.let { albumId ->
                playerBottomSheetViewModel.setLiveAlbum(
                    viewLifecycleOwner,
                    mediaType,
                    albumId
                )
            }

            extras.getString("artistId")?.let { artistId ->
                playerBottomSheetViewModel.setLiveArtist(
                    viewLifecycleOwner,
                    mediaType,
                    artistId
                )
            }
            playerBottomSheetViewModel.setLiveDescription(extras.getString("description", null))

            binding.playerHeaderLayout.playerHeaderMediaTitleLabel.text = extras.getString("title")
            binding.playerHeaderLayout.playerHeaderMediaArtistLabel.text =
                mediaMetadata.artist ?: if (extras.getString("type") == Constants.MEDIA_TYPE_RADIO) {
                    extras.getString("uri", getString(R.string.label_placeholder))
                } else {
                    ""
                }

            CustomGlideRequest.Builder
                .from(requireContext(), extras.getString("coverArtId"), CustomGlideRequest.ResourceType.Song)
                .build()
                .into(binding.playerHeaderLayout.playerHeaderMediaCoverImage)

            binding.playerHeaderLayout.playerHeaderMediaTitleLabel.visibility =
                if (!extras.getString("title").isNullOrBlank()) View.VISIBLE else View.GONE
            binding.playerHeaderLayout.playerHeaderMediaArtistLabel.visibility =
                if ((!extras.getString("artist").isNullOrBlank()) ||
                    (extras.getString("type") == Constants.MEDIA_TYPE_RADIO && extras.getString("uri") != null))
                    View.VISIBLE
                else
                    View.GONE
        }
    }

    private fun setMediaControllerUI(mediaBrowser: MediaBrowser) {
        mediaBrowser.mediaMetadata.extras?.let { extras ->
            when (extras.getString("type", Constants.MEDIA_TYPE_MUSIC)) {
                Constants.MEDIA_TYPE_PODCAST -> {
                    binding.playerHeaderLayout.playerHeaderFastForwardMediaButton.visibility = View.VISIBLE
                    binding.playerHeaderLayout.playerHeaderRewindMediaButton.visibility = View.VISIBLE
                    binding.playerHeaderLayout.playerHeaderNextMediaButton.visibility = View.GONE
                }
                else -> {
                    binding.playerHeaderLayout.playerHeaderFastForwardMediaButton.visibility = View.GONE
                    binding.playerHeaderLayout.playerHeaderRewindMediaButton.visibility = View.GONE
                    binding.playerHeaderLayout.playerHeaderNextMediaButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setContentDuration(duration: Long) {
        binding.playerHeaderLayout.playerHeaderSeekBar.max = (duration / 1000).toInt()
    }

    private fun setProgress(mediaBrowser: MediaBrowser) {
        _binding?.let { currentBinding ->
            currentBinding.playerHeaderLayout.playerHeaderSeekBar.setProgress((mediaBrowser.currentPosition / 1000).toInt(), true)
        }
    }

    private fun setPlayingState(isPlaying: Boolean) {
        binding.playerHeaderLayout.playerHeaderButton.isChecked = isPlaying
        runProgressBarHandler(isPlaying)
    }

    private fun setHeaderMediaController() {
        binding.playerHeaderLayout.playerHeaderButton.setOnClickListener {
            binding.root.findViewById<View>(R.id.exo_play_pause).performClick()
        }
        binding.playerHeaderLayout.playerHeaderNextMediaButton.setOnClickListener {
            binding.root.findViewById<View>(R.id.exo_next).performClick()
        }
        binding.playerHeaderLayout.playerHeaderRewindMediaButton.setOnClickListener {
            binding.root.findViewById<View>(R.id.exo_rew).performClick()
        }
        binding.playerHeaderLayout.playerHeaderFastForwardMediaButton.setOnClickListener {
            binding.root.findViewById<View>(R.id.exo_ffwd).performClick()
        }
    }

    private fun setHeaderNextButtonState(isEnabled: Boolean) {
        binding.playerHeaderLayout.playerHeaderNextMediaButton.isEnabled = isEnabled
        binding.playerHeaderLayout.playerHeaderNextMediaButton.alpha = if (isEnabled) 1.0f else 0.3f
    }

    fun goBackToFirstPage() {
        binding.playerBodyLayout.playerBodyBottomSheetViewPager.setCurrentItem(0, false)
        goToControllerPage()
    }

    fun goToControllerPage() {
        val playerControllerVerticalPager = binding.playerBodyLayout.playerBodyBottomSheetViewPager.adapter as? PlayerControllerVerticalPager
        playerControllerVerticalPager?.let { pager ->
            val playerControllerFragment = pager.getRegisteredFragment(0) as? PlayerControllerFragment
            playerControllerFragment?.goToControllerPage()
        }
    }

    fun goToLyricsPage() {
        val playerControllerVerticalPager = binding.playerBodyLayout.playerBodyBottomSheetViewPager.adapter as? PlayerControllerVerticalPager
        playerControllerVerticalPager?.let { pager ->
            val playerControllerFragment = pager.getRegisteredFragment(0) as? PlayerControllerFragment
            playerControllerFragment?.goToLyricsPage()
        }
    }

    fun goToQueuePage() {
        binding.playerBodyLayout.playerBodyBottomSheetViewPager.setCurrentItem(1, true)
    }

    fun setPlayerControllerVerticalPagerDraggableState(isDraggable: Boolean) {
        binding.playerBodyLayout.playerBodyBottomSheetViewPager.isUserInputEnabled = isDraggable
    }

    private fun defineProgressBarHandler(mediaBrowser: MediaBrowser) {
        progressBarHandler = Handler(Looper.getMainLooper())
        progressBarRunnable = Runnable {
            setProgress(mediaBrowser)
            progressBarHandler.postDelayed(progressBarRunnable, 1000)
        }
    }

    private fun runProgressBarHandler(isPlaying: Boolean) {
        if (isPlaying) {
            progressBarHandler.removeCallbacks(progressBarRunnable)
            progressBarHandler.postDelayed(progressBarRunnable, 1000)
        } else {
            progressBarHandler.removeCallbacks(progressBarRunnable)
        }
    }

    private fun setHeaderBookmarksButton() {
        if (Preferences.isSyncronizationEnabled()) {
            playerBottomSheetViewModel.playQueue.observeForever(object : Observer<PlayQueue?> {
                override fun onChanged(playQueue: PlayQueue?) {
                    playerBottomSheetViewModel.playQueue.removeObserver(this)

                    val currentBinding = _binding ?: return

                    val entries = playQueue?.entries.orEmpty()

                    if (entries.isNotEmpty()) {
                        val currentId = playQueue?.current
                        val index = currentId?.let { id -> entries.indexOfFirst { it.id == id } } ?: -1

                        if (index != -1) {
                            currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.visibility = View.VISIBLE
                            currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnClickListener {
                                MediaManager.startQueue(mediaBrowserListenableFuture, entries, index)
                                currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.visibility = View.GONE
                            }
                        }
                    } else {
                        currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.visibility = View.GONE
                        currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnClickListener(null)
                    }
                }
            })

            binding.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnLongClickListener {
                binding.playerHeaderLayout.playerHeaderBookmarkMediaButton.visibility = View.GONE
                true
            }

            Handler().postDelayed({
                _binding?.let { currentBinding ->
                    currentBinding.playerHeaderLayout.playerHeaderBookmarkMediaButton.visibility = View.GONE
                }
            }, Preferences.getSyncCountdownTimer() * 1000L)
        }
    }
}
