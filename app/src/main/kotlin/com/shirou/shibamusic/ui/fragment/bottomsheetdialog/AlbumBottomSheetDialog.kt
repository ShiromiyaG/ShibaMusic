package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
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
import com.shirou.shibamusic.interfaces.MediaCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.dialog.PlaylistChooserDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.AlbumBottomSheetViewModel
import com.shirou.shibamusic.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class AlbumBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var albumBottomSheetViewModel: AlbumBottomSheetViewModel
    private lateinit var album: AlbumID3

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_sheet_album_dialog, container, false)

        val args = requireArguments()
        album = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(args, Constants.ALBUM_OBJECT, AlbumID3::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(Constants.ALBUM_OBJECT)
        } ?: throw IllegalStateException("AlbumBottomSheetDialog requires an AlbumID3 argument")

        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        albumBottomSheetViewModel = ViewModelProvider(requireActivity())[AlbumBottomSheetViewModel::class.java]
        albumBottomSheetViewModel.album = album

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
        with(view) {
            val coverAlbum: ImageView = findViewById(R.id.album_cover_image_view)
            CustomGlideRequest.Builder
                .from(requireContext(), albumBottomSheetViewModel.album.coverArtId, CustomGlideRequest.ResourceType.Album)
                .build()
                .into(coverAlbum)

            val titleAlbum: TextView = findViewById(R.id.album_title_text_view)
            titleAlbum.text = albumBottomSheetViewModel.album.name
            titleAlbum.isSelected = true

            val artistAlbum: TextView = findViewById(R.id.album_artist_text_view)
            artistAlbum.text = albumBottomSheetViewModel.album.artist

            val favoriteToggle: ToggleButton = findViewById(R.id.button_favorite)
            favoriteToggle.isChecked = albumBottomSheetViewModel.album.starred != null
            favoriteToggle.setOnClickListener {
                albumBottomSheetViewModel.setFavorite(requireContext())
            }

            val playRadio: TextView = findViewById(R.id.play_radio_text_view)
            playRadio.setOnClickListener {
                val albumRepository = AlbumRepository()
                albumRepository.getInstantMix(album, 20, object : MediaCallback {
                    override fun onError(exception: Exception) {
                        exception.printStackTrace()
                    }

                    override fun onLoadMedia(media: List<*>) {
                        MusicUtil.ratingFilter(media as ArrayList<Child>)

                        if (media.isNotEmpty()) {
                            MediaManager.startQueue(mediaBrowserListenableFuture, media as ArrayList<Child>, 0)
                            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                        }

                        dismissBottomSheet()
                    }
                })
            }

            val playRandom: TextView = findViewById(R.id.play_random_text_view)
            playRandom.setOnClickListener {
                val albumId = album.id
                if (!albumId.isNullOrEmpty()) {
                    val albumRepository = AlbumRepository()
                    albumRepository.getAlbumTracks(albumId).observe(viewLifecycleOwner) { songs ->
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            MediaManager.startQueue(mediaBrowserListenableFuture, shuffled, 0)
                            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                        }

                        dismissBottomSheet()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_tracks), Toast.LENGTH_SHORT).show()
                    dismissBottomSheet()
                }
            }

            val playNext: TextView = findViewById(R.id.play_next_text_view)
            playNext.setOnClickListener {
                albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
                    MediaManager.enqueue(mediaBrowserListenableFuture, songs, true)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)

                    dismissBottomSheet()
                }
            }

            val addToQueue: TextView = findViewById(R.id.add_to_queue_text_view)
            addToQueue.setOnClickListener {
                albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
                    MediaManager.enqueue(mediaBrowserListenableFuture, songs, false)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)

                    dismissBottomSheet()
                }
            }

            val downloadAll: TextView = findViewById(R.id.download_all_text_view)
            albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
                val mediaItems = MappingUtil.mapDownloads(songs)
                val downloads = songs.map { Download(it) }

                downloadAll.setOnClickListener {
                    DownloadUtil.getDownloadTracker(requireContext()).download(mediaItems, downloads)
                    dismissBottomSheet()
                }
            }

            val addToPlaylist: TextView = findViewById(R.id.add_to_playlist_text_view)
            addToPlaylist.setOnClickListener {
                albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
                    val bundle = Bundle().apply {
                        putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(songs))
                    }

                    PlaylistChooserDialog().apply {
                        arguments = bundle
                    }.show(requireActivity().supportFragmentManager, null)

                    dismissBottomSheet()
                }
            }

            val removeAll: TextView = findViewById(R.id.remove_all_text_view)
            albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
                val mediaItems = MappingUtil.mapDownloads(songs)
                val downloads = songs.map { Download(it) }

                removeAll.setOnClickListener {
                    DownloadUtil.getDownloadTracker(requireContext()).remove(mediaItems, downloads)
                    dismissBottomSheet()
                }
            }

            initDownloadUI(removeAll)

            val goToArtist: TextView = findViewById(R.id.go_to_artist_text_view)
            goToArtist.setOnClickListener {
                albumBottomSheetViewModel.getArtist().observe(viewLifecycleOwner) { artist ->
                    if (artist != null) {
                        val bundle = Bundle().apply {
                            putParcelable(Constants.ARTIST_OBJECT, artist)
                        }
                        NavHostFragment.findNavController(this@AlbumBottomSheetDialog).navigate(R.id.artistPageFragment, bundle)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_artist), Toast.LENGTH_SHORT).show()
                    }

                    dismissBottomSheet()
                }
            }

            val share: TextView = findViewById(R.id.share_text_view)
            share.setOnClickListener {
                albumBottomSheetViewModel.shareAlbum().observe(viewLifecycleOwner) { sharedAlbum ->
                    if (sharedAlbum != null) {
                        val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText(getString(R.string.app_name), sharedAlbum.url)
                        clipboardManager.setPrimaryClip(clipData)
                        refreshShares()
                        dismissBottomSheet()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.share_unsupported_error), Toast.LENGTH_SHORT).show()
                        dismissBottomSheet()
                    }
                }
            }

            share.visibility = if (Preferences.isSharingEnabled()) View.VISIBLE else View.GONE
        }
    }

    override fun onClick(v: View) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun initDownloadUI(removeAll: TextView) {
        albumBottomSheetViewModel.getAlbumTracks().observe(viewLifecycleOwner) { songs ->
            val mediaItems = MappingUtil.mapDownloads(songs)

            if (DownloadUtil.getDownloadTracker(requireContext()).areDownloaded(mediaItems)) {
                removeAll.visibility = View.VISIBLE
            }
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
