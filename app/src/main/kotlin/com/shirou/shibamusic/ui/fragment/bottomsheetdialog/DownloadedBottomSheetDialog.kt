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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture
import java.util.Random

@UnstableApi
class DownloadedBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var songs: MutableList<Child>
    private lateinit var groupTitle: String
    private lateinit var groupSubtitle: String

    // mediaBrowserListenableFuture is initialized in onStart and released in onStop.
    // It can be null before onStart and after onStop.
    // The original Java code has a potential bug where this future might be null when accessed
    // by click listeners in init(), as init() is called in onCreateView() which runs before onStart().
    // This conversion preserves that original behavior by making it nullable and passing it as-is.
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_downloaded_dialog, container, false)

        val args = requireArguments()
        val tracks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelableArrayList(args, Constants.DOWNLOAD_GROUP, Child::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelableArrayList(Constants.DOWNLOAD_GROUP)
        } ?: throw IllegalStateException("DownloadedBottomSheetDialog requires a list of songs")
        songs = tracks.toMutableList()

        groupTitle = args.getString(Constants.DOWNLOAD_GROUP_TITLE).orEmpty()
        groupSubtitle = args.getString(Constants.DOWNLOAD_GROUP_SUBTITLE).orEmpty()

        initUI(view)
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

    private fun initUI(view: View) {
        with(view) {
            val playRandom: TextView = findViewById(R.id.play_random_text_view)
            playRandom.visibility = if (songs.size > 1) View.VISIBLE else View.GONE

            val remove: TextView = findViewById(R.id.remove_all_text_view)
            remove.text = if (songs.size > 1) {
                resources.getString(R.string.downloaded_bottom_sheet_remove_all)
            } else {
                resources.getString(R.string.downloaded_bottom_sheet_remove)
            }
        }
    }

    private fun init(view: View) {
        with(view) {
            val coverAlbum: ImageView = findViewById(R.id.group_cover_image_view)
            // Assuming 'songs' is never empty here, consistent with Java code's lack of checks.
            CustomGlideRequest.Builder.from(
                requireContext(),
                songs[Random().nextInt(songs.size)].coverArtId, // Kotlin's property access
                CustomGlideRequest.ResourceType.Unknown
            ).build().into(coverAlbum)

            val groupTitleView: TextView = findViewById(R.id.group_title_text_view)
            groupTitleView.apply {
                text = this@DownloadedBottomSheetDialog.groupTitle // Explicit `this` for clarity, though `groupTitle` alone works
                isSelected = true
            }

            val groupSubtitleView: TextView = findViewById(R.id.group_subtitle_text_view)
            groupSubtitleView.apply {
                text = this@DownloadedBottomSheetDialog.groupSubtitle
                isSelected = true
            }

            findViewById<TextView>(R.id.play_random_text_view).setOnClickListener {
                if (songs.isNotEmpty()) {
                    val shuffled = songs.shuffled()

                    MediaManager.startQueue(mediaBrowserListenableFuture, shuffled, 0)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                }

                dismissBottomSheet()
            }

            findViewById<TextView>(R.id.play_next_text_view).setOnClickListener {
                MediaManager.enqueue(mediaBrowserListenableFuture, songs.toList(), true)
                (requireActivity() as MainActivity).setBottomSheetInPeek(true)

                dismissBottomSheet()
            }

            findViewById<TextView>(R.id.add_to_queue_text_view).setOnClickListener {
                MediaManager.enqueue(mediaBrowserListenableFuture, songs.toList(), false)
                (requireActivity() as MainActivity).setBottomSheetInPeek(true)

                dismissBottomSheet()
            }

            findViewById<TextView>(R.id.remove_all_text_view).setOnClickListener {
                val mediaItems = MappingUtil.mapDownloads(songs)
                // Using Kotlin's collection map for transformation
                val downloads = songs.map { Download(it) }

                DownloadUtil.getDownloadTracker(requireContext()).remove(mediaItems, downloads)

                dismissBottomSheet()
            }
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
