package com.shirou.shibamusic.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.InnerFragmentPlayerLyricsBinding
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Line
import com.shirou.shibamusic.subsonic.models.LyricsList
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.OpenSubsonicExtensionsUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.PlayerBottomSheetViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.OptIn

@OptIn(UnstableApi::class)
class PlayerLyricsFragment : Fragment() {
    private val TAG = "PlayerLyricsFragment"

    private var _binding: InnerFragmentPlayerLyricsBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerBottomSheetViewModel: PlayerBottomSheetViewModel
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>
    private var mediaBrowser: MediaBrowser? = null
    private val syncLyricsHandler = Handler(Looper.getMainLooper())
    private var syncLyricsRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InnerFragmentPlayerLyricsBinding.inflate(inflater, container, false)
        val view = binding.root

        playerBottomSheetViewModel = ViewModelProvider(requireActivity())[PlayerBottomSheetViewModel::class.java]

        initOverlay()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPanelContent()
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
    }

    override fun onResume() {
        super.onResume()
        bindMediaController()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        releaseHandler()
    if (!Preferences.isDisplayAlwaysOn()) {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseHandler()
        _binding = null
    }

    private fun initOverlay() {
        binding.syncLyricsTapButton.setOnClickListener {
            playerBottomSheetViewModel.changeSyncLyricsState()
        }
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseHandler() {
        syncLyricsRunnable?.let { runnable ->
            syncLyricsHandler.removeCallbacks(runnable)
            syncLyricsRunnable = null
        }
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture.addListener({
            try {
                mediaBrowser = mediaBrowserListenableFuture.get()
                defineProgressHandler()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Error getting MediaBrowser: ${e.message}")
            }
        }, MoreExecutors.directExecutor())
    }

    private fun initPanelContent() {
        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable) {
            playerBottomSheetViewModel.liveLyricsList.observe(viewLifecycleOwner) { lyricsList ->
                setPanelContent(null, lyricsList)
            }
        } else {
            playerBottomSheetViewModel.liveLyrics.observe(viewLifecycleOwner) { lyrics ->
                setPanelContent(lyrics, null)
            }
        }
    }

    private fun setPanelContent(lyrics: String?, lyricsList: LyricsList?) {
        playerBottomSheetViewModel.liveDescription.observe(viewLifecycleOwner) { description ->
            _binding?.let { currentBinding ->
                currentBinding.nowPlayingSongLyricsSrollView.smoothScrollTo(0, 0)

                when {
                    !lyrics.isNullOrBlank() -> {
                        currentBinding.nowPlayingSongLyricsTextView.text = MusicUtil.getReadableLyrics(lyrics)
                        currentBinding.nowPlayingSongLyricsTextView.visibility = View.VISIBLE
                        currentBinding.emptyDescriptionImageView.visibility = View.GONE
                        currentBinding.titleEmptyDescriptionLabel.visibility = View.GONE
                        currentBinding.syncLyricsTapButton.visibility = View.GONE
                    }
                    lyricsList?.structuredLyrics?.isNotEmpty() == true -> {
                        setSyncLirics(lyricsList)
                        currentBinding.nowPlayingSongLyricsTextView.visibility = View.VISIBLE
                        currentBinding.emptyDescriptionImageView.visibility = View.GONE
                        currentBinding.titleEmptyDescriptionLabel.visibility = View.GONE
                        currentBinding.syncLyricsTapButton.visibility = View.VISIBLE
                    }
                    !description.isNullOrBlank() -> {
                        currentBinding.nowPlayingSongLyricsTextView.text = MusicUtil.getReadableLyrics(description)
                        currentBinding.nowPlayingSongLyricsTextView.visibility = View.VISIBLE
                        currentBinding.emptyDescriptionImageView.visibility = View.GONE
                        currentBinding.titleEmptyDescriptionLabel.visibility = View.GONE
                        currentBinding.syncLyricsTapButton.visibility = View.GONE
                    }
                    else -> {
                        currentBinding.nowPlayingSongLyricsTextView.visibility = View.GONE
                        currentBinding.emptyDescriptionImageView.visibility = View.VISIBLE
                        currentBinding.titleEmptyDescriptionLabel.visibility = View.VISIBLE
                        currentBinding.syncLyricsTapButton.visibility = View.GONE
                    }
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setSyncLirics(lyricsList: LyricsList) {
        val structured = lyricsList.structuredLyrics?.firstOrNull() ?: return
        val lines = structured.line.orEmpty().filterNotNull()
        if (lines.isEmpty()) return

        val lyricsBuilder = StringBuilder()
        lines.forEach { line ->
            lyricsBuilder.append(line.value.trim()).append("\n")
        }

        binding.nowPlayingSongLyricsTextView.text = lyricsBuilder.toString()
    }

    private fun defineProgressHandler() {
        playerBottomSheetViewModel.liveLyricsList.observe(viewLifecycleOwner) { lyricsList ->
            val structuredLyrics = lyricsList?.structuredLyrics

            if (structuredLyrics.isNullOrEmpty()) {
                releaseHandler()
                return@observe
            }

            if (structuredLyrics.firstOrNull()?.synced == false) {
                releaseHandler()
                return@observe
            }

            if (syncLyricsRunnable == null) {
                syncLyricsRunnable = object : Runnable {
                    override fun run() {
                        if (_binding != null) {
                            displaySyncedLyrics()
                            syncLyricsRunnable?.let { syncLyricsHandler.postDelayed(it, 250) }
                        }
                    }
                }
            }

            syncLyricsRunnable?.let { runnable ->
                syncLyricsHandler.removeCallbacks(runnable)
                syncLyricsHandler.postDelayed(runnable, 250)
            }
        }
    }

    private fun displaySyncedLyrics() {
        val lyricsList = playerBottomSheetViewModel.liveLyricsList.value
        val timestamp = mediaBrowser?.currentPosition?.toInt() ?: 0
        val structuredLyrics = lyricsList?.structuredLyrics?.firstOrNull() ?: return
        val lineList = structuredLyrics.line.orEmpty().filterNotNull()
        if (lineList.isEmpty()) return

        val lyricsBuilder = StringBuilder()
        lineList.forEach { line ->
            lyricsBuilder.append(line.value.trim()).append("\n")
        }

        val toHighlight = lineList.lastOrNull { line ->
            val start = line.start ?: return@lastOrNull false
            start < timestamp
        } ?: return

        val lyrics = lyricsBuilder.toString()
        val spannableString = SpannableString(lyrics)

        val startingPosition = getStartPosition(lineList, toHighlight)
        val endingPosition = startingPosition + toHighlight.value.length

        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.shadowsLyricsTextColor)),
            0,
            lyrics.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.lyricsTextColor)),
            startingPosition,
            endingPosition,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.nowPlayingSongLyricsTextView.text = spannableString

        if (playerBottomSheetViewModel.syncLyricsState) {
            binding.nowPlayingSongLyricsSrollView.smoothScrollTo(0, getScroll(lineList, toHighlight))
        }
    }

    private fun getStartPosition(lines: List<Line>, toHighlight: Line): Int {
        var start = 0
        for (line in lines) {
            if (line != toHighlight) {
                start += line.value.length + 1
            } else {
                break
            }
        }
        return start
    }

    private fun getLineCount(lines: List<Line>, toHighlight: Line): Int {
        var start = 0
        for (line in lines) {
            if (line != toHighlight) {
                binding.tempLyricsLineTextView.text = line.value
                start += binding.tempLyricsLineTextView.lineCount
            } else {
                break
            }
        }
        return start
    }

    private fun getScroll(lines: List<Line>, toHighlight: Line): Int {
        val startIndex = getStartPosition(lines, toHighlight)
        val layout = binding.nowPlayingSongLyricsTextView.layout ?: return 0

        val line = layout.getLineForOffset(startIndex)
        val lineTop = layout.getLineTop(line)
        val lineBottom = layout.getLineBottom(line)
        val lineCenter = (lineTop + lineBottom) / 2

        val scrollViewHeight = binding.nowPlayingSongLyricsSrollView.height
        val scroll = lineCenter - scrollViewHeight / 2

        return Math.max(scroll, 0)
    }
}
