package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.dialog.PlaylistChooserDialog
import com.shirou.shibamusic.ui.dialog.RatingDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.HomeViewModel
import com.shirou.shibamusic.viewmodel.SongBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class SongBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var songBottomSheetViewModel: SongBottomSheetViewModel
    private var song: Child? = null

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_song_dialog, container, false)

        val argumentSong = BundleCompat.getParcelable(requireArguments(), Constants.TRACK_OBJECT, Child::class.java)
        if (argumentSong == null) {
            dismissAllowingStateLoss()
            return view
        }
        song = argumentSong

        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        songBottomSheetViewModel = ViewModelProvider(requireActivity())[SongBottomSheetViewModel::class.java]
        songBottomSheetViewModel.song = argumentSong

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
        val currentSong = songBottomSheetViewModel.song ?: run {
            dismissAllowingStateLoss()
            return
        }

        val coverSong = view.findViewById<ImageView>(R.id.song_cover_image_view)
        CustomGlideRequest.Builder
            .from(requireContext(), currentSong.coverArtId, CustomGlideRequest.ResourceType.Song)
            .build()
            .into(coverSong)

        view.findViewById<TextView>(R.id.song_title_text_view).apply {
            text = currentSong.title.orEmpty()
            isSelected = true
        }

        view.findViewById<TextView>(R.id.song_artist_text_view).apply {
            text = currentSong.artist.orEmpty()
        }

        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.isChecked = currentSong.starred != null
        favoriteToggle.setOnClickListener {
            songBottomSheetViewModel.setFavorite(requireContext())
        }
        favoriteToggle.setOnLongClickListener {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, currentSong)
            }
            RatingDialog().apply {
                arguments = bundle
            }.show(requireActivity().supportFragmentManager, null)

            dismissBottomSheet()
            true
        }

        view.findViewById<TextView>(R.id.play_radio_text_view).setOnClickListener {
            MediaManager.startQueue(mediaBrowserListenableFuture, currentSong)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)

            songBottomSheetViewModel.getInstantMix(viewLifecycleOwner, currentSong).observe(viewLifecycleOwner) { songs ->
                MusicUtil.ratingFilter(songs)
                if (songs == null) {
                    dismissBottomSheet()
                    return@observe
                }
                if (songs.isNotEmpty()) {
                    MediaManager.enqueue(mediaBrowserListenableFuture, songs, true)
                    dismissBottomSheet()
                }
            }
        }

        view.findViewById<TextView>(R.id.play_next_text_view).setOnClickListener {
            MediaManager.enqueue(mediaBrowserListenableFuture, currentSong, true)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        view.findViewById<TextView>(R.id.add_to_queue_text_view).setOnClickListener {
            MediaManager.enqueue(mediaBrowserListenableFuture, currentSong, false)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        view.findViewById<TextView>(R.id.rate_text_view).setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(Constants.TRACK_OBJECT, currentSong)
            }
            RatingDialog().apply {
                arguments = bundle
            }.show(requireActivity().supportFragmentManager, null)

            dismissBottomSheet()
        }

        val downloadTextView = view.findViewById<TextView>(R.id.download_text_view)
        downloadTextView.setOnClickListener {
            DownloadUtil.getDownloadTracker(requireContext()).download(
                MappingUtil.mapDownload(currentSong),
                Download(currentSong)
            )
            dismissBottomSheet()
        }

        val removeTextView = view.findViewById<TextView>(R.id.remove_text_view)
        removeTextView.setOnClickListener {
            DownloadUtil.getDownloadTracker(requireContext()).remove(
                MappingUtil.mapDownload(currentSong),
                Download(currentSong)
            )
            dismissBottomSheet()
        }

        initDownloadUI(downloadTextView, removeTextView, currentSong)

        view.findViewById<TextView>(R.id.add_to_playlist_text_view).setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TRACKS_OBJECT, arrayListOf(currentSong))
            }
            PlaylistChooserDialog().apply {
                arguments = bundle
            }.show(requireActivity().supportFragmentManager, null)

            dismissBottomSheet()
        }

        val goToAlbumTextView = view.findViewById<TextView>(R.id.go_to_album_text_view)
        goToAlbumTextView.setOnClickListener {
            val albumLiveData = songBottomSheetViewModel.getAlbum()
            albumLiveData.observe(viewLifecycleOwner) { album ->
                if (album != null) {
                    val bundle = Bundle().apply {
                        putParcelable(Constants.ALBUM_OBJECT, album)
                    }
                    NavHostFragment.findNavController(this).navigate(R.id.albumPageFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.song_bottom_sheet_error_retrieving_album), Toast.LENGTH_SHORT).show()
                }
                albumLiveData.removeObservers(viewLifecycleOwner)
                dismissBottomSheet()
            }
        }
        goToAlbumTextView.visibility = if (!currentSong.albumId.isNullOrBlank()) View.VISIBLE else View.GONE

        val goToArtistTextView = view.findViewById<TextView>(R.id.go_to_artist_text_view)
        goToArtistTextView.setOnClickListener {
            val artistLiveData = songBottomSheetViewModel.getArtist()
            artistLiveData.observe(viewLifecycleOwner) { artist ->
                if (artist != null) {
                    val bundle = Bundle().apply {
                        putParcelable(Constants.ARTIST_OBJECT, artist)
                    }
                    NavHostFragment.findNavController(this).navigate(R.id.artistPageFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.song_bottom_sheet_error_retrieving_artist), Toast.LENGTH_SHORT).show()
                }
                artistLiveData.removeObservers(viewLifecycleOwner)
                dismissBottomSheet()
            }
        }
        goToArtistTextView.visibility = if (!currentSong.artistId.isNullOrBlank()) View.VISIBLE else View.GONE

        val shareTextView = view.findViewById<TextView>(R.id.share_text_view)
        shareTextView.setOnClickListener {
            songBottomSheetViewModel.shareTrack().observe(viewLifecycleOwner) { sharedTrack ->
                if (sharedTrack != null) {
                    val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(getString(R.string.app_name), sharedTrack.url)
                    clipboardManager.setPrimaryClip(clipData)
                    refreshShares()
                    dismissBottomSheet()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.share_unsupported_error), Toast.LENGTH_SHORT).show()
                    dismissBottomSheet()
                }
            }
        }
        shareTextView.visibility = if (Preferences.isSharingEnabled()) View.VISIBLE else View.GONE
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun initDownloadUI(download: TextView, remove: TextView, song: Child) {
        if (DownloadUtil.getDownloadTracker(requireContext()).isDownloaded(song.id)) {
            remove.visibility = View.VISIBLE
            download.visibility = View.GONE
        } else {
            download.visibility = View.VISIBLE
            remove.visibility = View.GONE
        }
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture)
    }

    private fun refreshShares() {
        homeViewModel.refreshShares(requireActivity())
    }
}
