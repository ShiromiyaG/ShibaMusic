@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentPlaylistPageBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.viewmodel.PlaylistPageViewModel
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PlaylistPageFragment : Fragment(), ClickCallback {

    private var _binding: FragmentPlaylistPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var playlistPageViewModel: PlaylistPageViewModel
    private lateinit var songHorizontalAdapter: SongHorizontalAdapter
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.playlist_page_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                songHorizontalAdapter.filter.filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)

        initMenuOption(menu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentPlaylistPageBinding.inflate(inflater, container, false)
        val view = binding.root
        playlistPageViewModel = ViewModelProvider(requireActivity())[PlaylistPageViewModel::class.java]

        init()
        initAppBar()
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
            R.id.action_download_playlist -> {
                playlistPageViewModel.getPlaylistSongLiveList().observe(viewLifecycleOwner) { songs ->
                    val nonNullSongs = songs?.takeIf { it.isNotEmpty() } ?: return@observe

                    if (isVisible) {
                        DownloadUtil.getDownloadTracker(requireContext()).download(
                            MappingUtil.mapDownloads(nonNullSongs),
                            nonNullSongs.map { child ->
                                Download(child.id).apply {
                                    playlistId = playlistPageViewModel.playlist.id
                                    playlistName = playlistPageViewModel.playlist.name
                                }
                            }
                        )
                    }
                }
                true
            }
            R.id.action_pin_playlist -> {
                playlistPageViewModel.setPinned(true)
                true
            }
            R.id.action_unpin_playlist -> {
                playlistPageViewModel.setPinned(false)
                true
            }
            else -> false
        }
    }

    private fun init() {
        val playlist: Playlist? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(
                requireArguments(),
                Constants.PLAYLIST_OBJECT,
                Playlist::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(Constants.PLAYLIST_OBJECT)
        }
        playlistPageViewModel.playlist = requireNotNull(playlist) { "Playlist argument is required" }
    }

    private fun initMenuOption(menu: Menu) {
        playlistPageViewModel.isPinned(viewLifecycleOwner).observe(viewLifecycleOwner) { isPinned ->
            menu.findItem(R.id.action_unpin_playlist)?.isVisible = isPinned
            menu.findItem(R.id.action_pin_playlist)?.isVisible = !isPinned
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.animToolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.animToolbar.title = playlistPageViewModel.playlist.name

        binding.playlistNameLabel.text = playlistPageViewModel.playlist.name
        binding.playlistSongCountLabel.text = getString(R.string.playlist_song_count, playlistPageViewModel.playlist.songCount)
        binding.playlistDurationLabel.text = getString(R.string.playlist_duration, MusicUtil.getReadableDurationString(playlistPageViewModel.playlist.duration, false))

        binding.animToolbar.setNavigationOnClickListener { v ->
            hideKeyboard(v)
            activity.navController.navigateUp()
        }

        binding.animToolbar.overflowIcon?.setTint(
            requireContext().resources.getColor(R.color.titleTextColor, requireContext().theme)
        )
    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun initMusicButton() {
    playlistPageViewModel.getPlaylistSongLiveList().observe(viewLifecycleOwner) { songs ->
            _binding?.let { currentBinding ->
                currentBinding.playlistPagePlayButton.setOnClickListener {
                    songs?.let { songList ->
                        MediaManager.startQueue(mediaBrowserListenableFuture, songList, 0)
                    }
                }

                currentBinding.playlistPageShuffleButton.setOnClickListener {
                    songs?.toMutableList()?.let { mutableSongs ->
                        mutableSongs.shuffle()
                        MediaManager.startQueue(mediaBrowserListenableFuture, mutableSongs, 0)
                    }
                }
            }
        }
    }

    private fun initBackCover() {
    playlistPageViewModel.getPlaylistSongLiveList().observe(viewLifecycleOwner) { songs ->
            _binding?.let { currentBinding ->
                if (!songs.isNullOrEmpty()) {
                    val mutableSongs = songs.toMutableList()
                    mutableSongs.shuffle()

                    val playlistCoverArtId = playlistPageViewModel.playlist.coverArtId

                    // Pic top-left
                    CustomGlideRequest.Builder
                        .from(
                            requireContext(),
                            mutableSongs.getOrNull(0)?.coverArtId ?: playlistCoverArtId,
                            CustomGlideRequest.ResourceType.Song
                        )
                        .build()
                        .transform(GranularRoundedCorners(CustomGlideRequest.CORNER_RADIUS.toFloat(), 0f, 0f, 0f))
                        .into(currentBinding.playlistCoverImageViewTopLeft)

                    // Pic top-right
                    CustomGlideRequest.Builder
                        .from(
                            requireContext(),
                            mutableSongs.getOrNull(1)?.coverArtId ?: playlistCoverArtId,
                            CustomGlideRequest.ResourceType.Song
                        )
                        .build()
                        .transform(GranularRoundedCorners(0f, CustomGlideRequest.CORNER_RADIUS.toFloat(), 0f, 0f))
                        .into(currentBinding.playlistCoverImageViewTopRight)

                    // Pic bottom-left
                    CustomGlideRequest.Builder
                        .from(
                            requireContext(),
                            mutableSongs.getOrNull(2)?.coverArtId ?: playlistCoverArtId,
                            CustomGlideRequest.ResourceType.Song
                        )
                        .build()
                        .transform(GranularRoundedCorners(0f, 0f, 0f, CustomGlideRequest.CORNER_RADIUS.toFloat()))
                        .into(currentBinding.playlistCoverImageViewBottomLeft)

                    // Pic bottom-right
                    CustomGlideRequest.Builder
                        .from(
                            requireContext(),
                            mutableSongs.getOrNull(3)?.coverArtId ?: playlistCoverArtId,
                            CustomGlideRequest.ResourceType.Song
                        )
                        .build()
                        .transform(GranularRoundedCorners(0f, 0f, CustomGlideRequest.CORNER_RADIUS.toFloat(), 0f))
                        .into(currentBinding.playlistCoverImageViewBottomRight)
                }
            }
        }
    }

    private fun initSongsView() {
        binding.songRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.songRecyclerView.setHasFixedSize(true)

        songHorizontalAdapter = SongHorizontalAdapter(this, true, false, null)
        binding.songRecyclerView.adapter = songHorizontalAdapter

    playlistPageViewModel.getPlaylistSongLiveList().observe(viewLifecycleOwner) { songs ->
            songs?.let { songHorizontalAdapter.setItems(it) }
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
        val tracks = bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT)
        val position = bundle.getInt(Constants.ITEM_POSITION)

        tracks?.let {
            MediaManager.startQueue(mediaBrowserListenableFuture, it, position)
        }
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView())?.navigate(R.id.songBottomSheetDialog, bundle)
    }
}
