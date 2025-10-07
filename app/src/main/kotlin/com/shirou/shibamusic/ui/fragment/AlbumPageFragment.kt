package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentAlbumPageBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.ui.dialog.PlaylistChooserDialog
import com.shirou.shibamusic.ui.dialog.RatingDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.viewmodel.AlbumPageViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayList
import java.util.Collections

@UnstableApi
class AlbumPageFragment : Fragment(), ClickCallback {
    private var _binding: FragmentAlbumPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var activity: MainActivity
    private lateinit var albumPageViewModel: AlbumPageViewModel
    private lateinit var songHorizontalAdapter: SongHorizontalAdapter
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.album_page_menu, menu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentAlbumPageBinding.inflate(inflater, container, false)
        val view = binding.root
        albumPageViewModel = ViewModelProvider(requireActivity()).get(AlbumPageViewModel::class.java)

        init()
        initAppBar()
        initAlbumInfoTextButton()
        initAlbumNotes()
        initMusicButton()
        initBackCover()
        initSongsView()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rate_album -> {
                val bundle = Bundle()
                val album = albumPageViewModel.album.value
                album?.let {
                    bundle.putParcelable(Constants.ALBUM_OBJECT, it)
                }
                RatingDialog().apply {
                    arguments = bundle
                }.show(requireActivity().supportFragmentManager, null)
                true
            }
            R.id.action_download_album -> {
                val songs = albumPageViewModel.albumSongLiveList.value
                if (!songs.isNullOrEmpty()) {
                    DownloadUtil.getDownloadTracker(requireContext()).download(MappingUtil.mapDownloads(songs), songs.map { Download(it) })
                } else {
                    Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_tracks), Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_add_to_playlist -> {
                val songs = albumPageViewModel.albumSongLiveList.value
                if (!songs.isNullOrEmpty()) {
                    val bundle = Bundle()
                    bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(songs))

                    PlaylistChooserDialog().apply {
                        arguments = bundle
                    }.show(requireActivity().supportFragmentManager, null)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_tracks), Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> false
        }
    }

    private fun init() {
        @Suppress("DEPRECATION")
        val albumArgument: AlbumID3? = arguments?.getParcelable(Constants.ALBUM_OBJECT)
        albumPageViewModel.setAlbum(viewLifecycleOwner, albumArgument)

        if (albumArgument == null) {
            Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_album), Toast.LENGTH_SHORT).show()
            activity.navigateUpIfPossible()
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.animToolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        albumPageViewModel.album.observe(viewLifecycleOwner) { album ->
            album?.let {
                binding.apply {
                    animToolbar.title = it.name
                    albumNameLabel.text = it.name
                    albumArtistLabel.text = it.artist
                    albumReleaseYearLabel.text = if (it.year != 0) it.year.toString() else ""
                    albumReleaseYearLabel.visibility = if (it.year != 0) View.VISIBLE else View.GONE
                    albumSongCountDurationTextview.text = getString(R.string.album_page_tracks_count_and_duration, it.songCount, it.duration?.div(60) ?: 0)

                    if (!it.genre.isNullOrEmpty()) {
                        albumGenresTextview.text = it.genre
                        albumGenresTextview.visibility = View.VISIBLE
                    } else {
                        albumGenresTextview.visibility = View.GONE
                    }

                    val releaseDate = it.releaseDate
                    val originalReleaseDate = it.originalReleaseDate
                    
                    if (releaseDate != null && originalReleaseDate != null) {
                        val releaseFormatted = releaseDate.getFormattedDate()
                        val originalFormatted = originalReleaseDate.getFormattedDate()
                        
                        if (releaseFormatted != null || originalFormatted != null) {
                            albumReleaseYearsTextview.visibility = View.VISIBLE
                        } else {
                            albumReleaseYearsTextview.visibility = View.GONE
                        }

                        if (releaseFormatted == null || originalFormatted == null) {
                            albumReleaseYearsTextview.text = getString(R.string.album_page_release_date_label, releaseFormatted ?: originalFormatted)
                        }

                        if (releaseFormatted != null && originalFormatted != null) {
                            if (releaseDate.year == originalReleaseDate.year && releaseDate.month == originalReleaseDate.month && releaseDate.day == originalReleaseDate.day) {
                                albumReleaseYearsTextview.text = getString(R.string.album_page_release_date_label, releaseFormatted)
                            } else {
                                albumReleaseYearsTextview.text = getString(R.string.album_page_release_dates_label, releaseFormatted, originalFormatted)
                            }
                        }
                    }
                }
            }
        }

    binding.animToolbar.setNavigationOnClickListener { activity.navigateUpIfPossible() }

        binding.animToolbar.overflowIcon?.setTint(requireContext().getColor(R.color.titleTextColor))

        binding.albumOtherInfoButton.setOnClickListener {
            binding.albumDetailView.visibility =
                if (binding.albumDetailView.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun initAlbumInfoTextButton() {
        binding.albumArtistLabel.setOnClickListener {
            albumPageViewModel.artist.observe(viewLifecycleOwner) { artist ->
                artist?.let {
                    val bundle = Bundle()
                    bundle.putParcelable(Constants.ARTIST_OBJECT, it)
                    activity.navigateIfPossible(R.id.action_albumPageFragment_to_artistPageFragment, bundle)
                } ?: run {
                    Toast.makeText(requireContext(), getString(R.string.album_error_retrieving_artist), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initAlbumNotes() {
        albumPageViewModel.albumInfo.observe(viewLifecycleOwner) { albumInfo ->
            binding.albumNotesTextview.apply {
                if (albumInfo != null) {
                    visibility = View.VISIBLE
                    text = MusicUtil.forceReadableString(albumInfo.notes)

                    albumInfo.lastFmUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                        setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        }
                    } ?: run {
                        // If no URL, clear any previous click listener
                        setOnClickListener(null)
                    }
                } else {
                    visibility = View.GONE
                    setOnClickListener(null) // Clear click listener when notes are gone
                }
            }
        }
    }

    private fun initMusicButton() {
        albumPageViewModel.albumSongLiveList.observe(viewLifecycleOwner) { songs ->
            val songList = songs ?: emptyList()
            binding.apply {
                if (songList.isNotEmpty()) {
                    albumPagePlayButton.isEnabled = true
                    albumPagePlayButton.setOnClickListener {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songList, 0)
                        // activity.isBottomSheetInPeek = true
                    }

                    albumPageShuffleButton.isEnabled = true
                    albumPageShuffleButton.setOnClickListener {
                        // Create a mutable copy for shuffling
                        val shuffledSongs = songList.toMutableList()
                        Collections.shuffle(shuffledSongs) // In-place shuffle on mutable list
                        MediaManager.startQueue(mediaBrowserListenableFuture, shuffledSongs, 0)
                        // activity.isBottomSheetInPeek = true
                    }
                } else {
                    albumPagePlayButton.isEnabled = false
                    albumPageShuffleButton.isEnabled = false
                    // Clear click listeners when disabled
                    albumPagePlayButton.setOnClickListener(null)
                    albumPageShuffleButton.setOnClickListener(null)
                }
            }
        }
    }

    private fun initBackCover() {
        albumPageViewModel.album.observe(viewLifecycleOwner) { album ->
            album?.let {
                CustomGlideRequest.Builder.from(requireContext(), it.coverArtId, CustomGlideRequest.ResourceType.Album).build().into(binding.albumCoverImageView)
            }
        }
    }

    private fun initSongsView() {
        albumPageViewModel.album.observe(viewLifecycleOwner) { album ->
            album?.let { albumID3 ->
                binding.songRecyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    setHasFixedSize(true)
                    songHorizontalAdapter = SongHorizontalAdapter(this@AlbumPageFragment, false, false, albumID3)
                    adapter = songHorizontalAdapter
                }
                albumPageViewModel.albumSongLiveList.observe(viewLifecycleOwner) { songs ->
                    songHorizontalAdapter.setItems(songs ?: emptyList())
                }
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

    override fun onMediaClick(bundle: Bundle) {
    val tracks = bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT) ?: emptyList()
    MediaManager.startQueue(mediaBrowserListenableFuture, tracks, bundle.getInt(Constants.ITEM_POSITION))
        // activity.isBottomSheetInPeek = true
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }
}
