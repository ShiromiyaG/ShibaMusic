package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import android.widget.RatingBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.InnerFragmentPlayerControllerBinding
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.dialog.RatingDialog
import com.shirou.shibamusic.ui.dialog.TrackInfoDialog
import com.shirou.shibamusic.ui.fragment.pager.PlayerControllerHorizontalPager
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.PlayerBottomSheetViewModel
import com.shirou.shibamusic.viewmodel.RatingViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.text.DecimalFormat

@UnstableApi
class PlayerControllerFragment : Fragment() {
    private val TAG = "PlayerCoverFragment"

    private var bind: InnerFragmentPlayerControllerBinding? = null
    private lateinit var playerMediaCoverViewPager: ViewPager2
    private lateinit var buttonFavorite: ToggleButton
    private lateinit var ratingViewModel: RatingViewModel
    private lateinit var songRatingBar: RatingBar
    private lateinit var playerMediaTitleLabel: TextView
    private lateinit var playerArtistNameLabel: TextView
    private lateinit var playbackSpeedButton: Button
    private lateinit var skipSilenceToggleButton: ToggleButton
    private lateinit var playerMediaExtension: Chip
    private lateinit var playerMediaBitrate: TextView
    private lateinit var playerQuickActionView: ConstraintLayout
    private lateinit var playerOpenQueueButton: ImageButton
    private lateinit var playerTrackInfo: ImageButton
    private lateinit var ratingContainer: LinearLayout

    private lateinit var activity: MainActivity
    private lateinit var playerBottomSheetViewModel: PlayerBottomSheetViewModel
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        bind = InnerFragmentPlayerControllerBinding.inflate(inflater, container, false)
        val view = bind!!.root

        playerBottomSheetViewModel = ViewModelProvider(requireActivity()).get(PlayerBottomSheetViewModel::class.java)
        ratingViewModel = ViewModelProvider(requireActivity()).get(RatingViewModel::class.java)

        init()
        initQuickActionView()
        initCoverLyricsSlideView()
        initMediaListenable()
        initMediaLabelButton()
        initArtistLabelButton()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
        bindMediaController()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        // Accessing bind with !! because it's guaranteed non-null after onCreateView and before this call
        val root = bind!!.root
        playerMediaCoverViewPager = root.findViewById(R.id.player_media_cover_view_pager)
        buttonFavorite = root.findViewById(R.id.button_favorite)
        playerMediaTitleLabel = root.findViewById(R.id.player_media_title_label)
        playerArtistNameLabel = root.findViewById(R.id.player_artist_name_label)
        playbackSpeedButton = root.findViewById(R.id.player_playback_speed_button)
        skipSilenceToggleButton = root.findViewById(R.id.player_skip_silence_toggle_button)
        playerMediaExtension = root.findViewById(R.id.player_media_extension)
        playerMediaBitrate = root.findViewById(R.id.player_media_bitrate)
        playerQuickActionView = root.findViewById(R.id.player_quick_action_view)
        playerOpenQueueButton = root.findViewById(R.id.player_open_queue_button)
        playerTrackInfo = root.findViewById(R.id.player_info_track)
        songRatingBar =  root.findViewById(R.id.song_rating_bar)
        ratingContainer = root.findViewById(R.id.rating_container)
        checkAndSetRatingContainerVisibility()
    }

    private fun initQuickActionView() {
        playerQuickActionView.setBackgroundColor(SurfaceColors.getColorForElevation(requireContext(), 8f))

        playerOpenQueueButton.setOnClickListener {
            val playerBottomSheetFragment = requireActivity().supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment
            playerBottomSheetFragment?.goToQueuePage()
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

                bind?.nowPlayingMediaControllerView?.player = mediaBrowser
                mediaBrowser.shuffleModeEnabled = Preferences.isShuffleModeEnabled()
                mediaBrowser.repeatMode = Preferences.getRepeatMode()
                setMediaControllerListener(mediaBrowser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaControllerListener(mediaBrowser: MediaBrowser) {
        setMediaControllerUI(mediaBrowser)
        setMetadata(mediaBrowser.mediaMetadata)
        setMediaInfo(mediaBrowser.mediaMetadata)

        mediaBrowser.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                setMediaControllerUI(mediaBrowser)
                setMetadata(mediaMetadata)
                setMediaInfo(mediaMetadata)
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
        playerMediaTitleLabel.text = mediaMetadata.title?.toString()

        val extras = mediaMetadata.extras

        playerArtistNameLabel.text =
            when {
                !mediaMetadata.artist.isNullOrEmpty() -> mediaMetadata.artist.toString()
                extras?.getString("type") == Constants.MEDIA_TYPE_RADIO ->
                    extras.getString("uri") ?: getString(R.string.label_placeholder)
                else -> ""
            }

        playerMediaTitleLabel.isSelected = true
        playerArtistNameLabel.isSelected = true

        playerMediaTitleLabel.visibility = if (!mediaMetadata.title.isNullOrEmpty()) View.VISIBLE else View.GONE

        val radioUri = extras?.getString("uri")
        val hasRadioUri = extras?.getString("type") == Constants.MEDIA_TYPE_RADIO && !radioUri.isNullOrEmpty()

        playerArtistNameLabel.visibility =
            if (!mediaMetadata.artist.isNullOrEmpty() || hasRadioUri) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun setMediaInfo(mediaMetadata: MediaMetadata) {
        mediaMetadata.extras?.let { extras ->
            val extension = extras.getString("suffix", getString(R.string.player_unknown_format))
            val bitrateVal = extras.getInt("bitrate", 0)
            val bitrate = if (bitrateVal != 0) "${bitrateVal}kbps" else "Original"
            val samplingRateVal = extras.getInt("samplingRate", 0)
            val samplingRate = if (samplingRateVal != 0) "${DecimalFormat("0.#").format(samplingRateVal / 1000.0)}kHz" else ""
            val bitDepthVal = extras.getInt("bitDepth", 0)
            val bitDepth = if (bitDepthVal != 0) "${bitDepthVal}b" else ""

            playerMediaExtension.text = extension

            if (bitrate == "Original") {
                playerMediaBitrate.visibility = View.GONE
            } else {
                val mediaQualityItems = mutableListOf<String>()

                if (bitrate.trim().isNotEmpty()) mediaQualityItems.add(bitrate)
                if (bitDepth.trim().isNotEmpty()) mediaQualityItems.add(bitDepth)
                if (samplingRate.trim().isNotEmpty()) mediaQualityItems.add(samplingRate)

                val mediaQuality = mediaQualityItems.joinToString(" • ")
                playerMediaBitrate.visibility = View.VISIBLE
                playerMediaBitrate.text = mediaQuality
            }
        }

        val isTranscodingExtension = MusicUtil.getTranscodingFormatPreference() != "raw"
        val isTranscodingBitrate = MusicUtil.getBitratePreference() != "0"

        if (isTranscodingExtension || isTranscodingBitrate) {
            playerMediaExtension.text = "${MusicUtil.getTranscodingFormatPreference()} (${getString(R.string.player_transcoding)})"
            playerMediaBitrate.text = if (MusicUtil.getBitratePreference() != "0") "${MusicUtil.getBitratePreference()}kbps" else getString(R.string.player_transcoding_requested)
        }

        playerTrackInfo.setOnClickListener {
            val dialog = TrackInfoDialog(mediaMetadata)
            dialog.show(activity.supportFragmentManager, null)
        }
    }

    private fun setMediaControllerUI(mediaBrowser: MediaBrowser) {
        initPlaybackSpeedButton(mediaBrowser)

        bind?.root?.let { root ->
            mediaBrowser.mediaMetadata.extras?.getString("type", Constants.MEDIA_TYPE_MUSIC)?.let { type ->
                when (type) {
                    Constants.MEDIA_TYPE_PODCAST -> {
                        root.setShowShuffleButton(false)
                        root.setShowRewindButton(true)
                        root.setShowPreviousButton(false)
                        root.setShowNextButton(false)
                        root.setShowFastForwardButton(true)
                        root.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE)
                        root.findViewById<View>(R.id.player_playback_speed_button).visibility = View.VISIBLE
                        root.findViewById<View>(R.id.player_skip_silence_toggle_button).visibility = View.VISIBLE
                        root.findViewById<View>(R.id.button_favorite).visibility = View.GONE
                        setPlaybackParameters(mediaBrowser)
                    }
                    Constants.MEDIA_TYPE_RADIO -> {
                        root.setShowShuffleButton(false)
                        root.setShowRewindButton(false)
                        root.setShowPreviousButton(false)
                        root.setShowNextButton(false)
                        root.setShowFastForwardButton(false)
                        root.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE)
                        root.findViewById<View>(R.id.player_playback_speed_button).visibility = View.GONE
                        root.findViewById<View>(R.id.player_skip_silence_toggle_button).visibility = View.GONE
                        root.findViewById<View>(R.id.button_favorite).visibility = View.GONE
                        setPlaybackParameters(mediaBrowser)
                    }
                    else -> {
                        root.setShowShuffleButton(true)
                        root.setShowRewindButton(false)
                        root.setShowPreviousButton(true)
                        root.setShowNextButton(true)
                        root.setShowFastForwardButton(false)
                        root.setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL or RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
                        root.findViewById<View>(R.id.player_playback_speed_button).visibility = View.GONE
                        root.findViewById<View>(R.id.player_skip_silence_toggle_button).visibility = View.GONE
                        root.findViewById<View>(R.id.button_favorite).visibility = View.VISIBLE
                        resetPlaybackParameters(mediaBrowser)
                    }
                }
            }
        }
    }

    private fun initCoverLyricsSlideView() {
        playerMediaCoverViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        playerMediaCoverViewPager.adapter = PlayerControllerHorizontalPager(this)

        playerMediaCoverViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val playerBottomSheetFragment = requireActivity().supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment

                if (position == 0) {
                    activity.setBottomSheetDraggableState(true)
                    playerBottomSheetFragment?.setPlayerControllerVerticalPagerDraggableState(true)
                } else if (position == 1) {
                    activity.setBottomSheetDraggableState(false)
                    playerBottomSheetFragment?.setPlayerControllerVerticalPagerDraggableState(false)
                }
            }
        })
    }

    private fun initMediaListenable() {
        playerBottomSheetViewModel.liveMedia.observe(viewLifecycleOwner) { media ->
            media?.let {
                ratingViewModel.song = it
                buttonFavorite.isChecked = it.starred != null
                buttonFavorite.setOnClickListener { _ -> playerBottomSheetViewModel.setFavorite(requireContext(), it) }
                buttonFavorite.setOnLongClickListener { _ ->
                    val bundle = Bundle().apply {
                        putParcelable(Constants.TRACK_OBJECT, it)
                    }

                    val dialog = RatingDialog().apply {
                        arguments = bundle
                    }
                    dialog.show(requireActivity().supportFragmentManager, null)
                    true
                }

                val currentRating: Int? = it.userRating

                songRatingBar.rating = currentRating?.toFloat() ?: 0f

                songRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser) {
                        ratingViewModel.rate(rating.toInt())
                        it.userRating = rating.toInt()
                    }
                }
                playerBottomSheetViewModel.refreshMediaInfo(requireActivity(), it)
            }
        }
    }

    private fun initMediaLabelButton() {
        playerBottomSheetViewModel.liveAlbum.observe(viewLifecycleOwner) { album ->
            album?.let {
                playerMediaTitleLabel.setOnClickListener { _ ->
                    val bundle = Bundle().apply {
                        putParcelable(Constants.ALBUM_OBJECT, it)
                    }
                    NavHostFragment.findNavController(this).navigate(R.id.albumPageFragment, bundle)
                    activity.collapseBottomSheetDelayed()
                }
            }
        }
    }

    private fun initArtistLabelButton() {
        playerBottomSheetViewModel.liveArtist.observe(viewLifecycleOwner) { artist ->
            artist?.let {
                playerArtistNameLabel.setOnClickListener { _ ->
                    val bundle = Bundle().apply {
                        putParcelable(Constants.ARTIST_OBJECT, it)
                    }
                    NavHostFragment.findNavController(this).navigate(R.id.artistPageFragment, bundle)
                    activity.collapseBottomSheetDelayed()
                }
            }
        }
    }

    private fun initPlaybackSpeedButton(mediaBrowser: MediaBrowser) {
        playbackSpeedButton.setOnClickListener {
            val currentSpeed = Preferences.getPlaybackSpeed()
            val newSpeed: Float

            when (currentSpeed) {
                Constants.MEDIA_PLAYBACK_SPEED_080 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_100
                Constants.MEDIA_PLAYBACK_SPEED_100 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_125
                Constants.MEDIA_PLAYBACK_SPEED_125 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_150
                Constants.MEDIA_PLAYBACK_SPEED_150 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_175
                Constants.MEDIA_PLAYBACK_SPEED_175 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_200
                Constants.MEDIA_PLAYBACK_SPEED_200 -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_080
                else -> newSpeed = Constants.MEDIA_PLAYBACK_SPEED_100 // Default case
            }
            mediaBrowser.playbackParameters = PlaybackParameters(newSpeed)
            playbackSpeedButton.text = getString(R.string.player_playback_speed, newSpeed)
            Preferences.setPlaybackSpeed(newSpeed)
        }

        skipSilenceToggleButton.setOnClickListener {
            Preferences.setSkipSilenceMode(skipSilenceToggleButton.isChecked)
        }
    }

    fun goToControllerPage() {
        playerMediaCoverViewPager.setCurrentItem(0, false)
    }

    fun goToLyricsPage() {
        playerMediaCoverViewPager.setCurrentItem(1, true)
    }

    private fun checkAndSetRatingContainerVisibility() {
        if (Preferences.showItemStarRating()) {
            ratingContainer.visibility = View.VISIBLE
        } else {
            ratingContainer.visibility = View.GONE
        }
    }

    private fun setPlaybackParameters(mediaBrowser: MediaBrowser) {
        val currentSpeed = Preferences.getPlaybackSpeed()
        val skipSilence = Preferences.isSkipSilenceMode()

        mediaBrowser.playbackParameters = PlaybackParameters(currentSpeed)
        playbackSpeedButton.text = getString(R.string.player_playback_speed, currentSpeed)

        // TODO Skippare il silenzio
        skipSilenceToggleButton.isChecked = skipSilence
    }

    private fun resetPlaybackParameters(mediaBrowser: MediaBrowser) {
        mediaBrowser.playbackParameters = PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_100)
        // TODO Resettare lo skip del silenzio
    }
}
