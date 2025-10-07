package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentLibraryBinding
import com.shirou.shibamusic.helper.recyclerview.CustomLinearSnapHelper
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.PlaylistCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumAdapter
import com.shirou.shibamusic.ui.adapter.ArtistAdapter
import com.shirou.shibamusic.ui.adapter.GenreAdapter
import com.shirou.shibamusic.ui.adapter.MusicFolderAdapter
import com.shirou.shibamusic.ui.adapter.PlaylistHorizontalAdapter
import com.shirou.shibamusic.ui.dialog.PlaylistEditorDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.LibraryViewModel
import com.google.android.material.appbar.MaterialToolbar

@UnstableApi
class LibraryFragment : Fragment(), ClickCallback {
    private val TAG = "LibraryFragment"

    private var _binding: FragmentLibraryBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var libraryViewModel: LibraryViewModel

    private lateinit var musicFolderAdapter: MusicFolderAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var genreAdapter: GenreAdapter
    private lateinit var playlistHorizontalAdapter: PlaylistHorizontalAdapter

    private lateinit var materialToolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        val view = binding.root
        libraryViewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]

        init()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initMusicFolderView()
        initAlbumView()
        initArtistView()
        initGenreView()
        initPlaylistView()
    }

    override fun onStart() {
        super.onStart()
        activity.setBottomNavigationBarVisibility(true)
    }

    override fun onResume() {
        super.onResume()
        refreshPlaylistView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        binding.albumCatalogueTextViewClickable.setOnClickListener {
            activity.navController.navigate(R.id.action_libraryFragment_to_albumCatalogueFragment)
        }
        binding.artistCatalogueTextViewClickable.setOnClickListener {
            activity.navController.navigate(R.id.action_libraryFragment_to_artistCatalogueFragment)
        }
        binding.genreCatalogueTextViewClickable.setOnClickListener {
            activity.navController.navigate(R.id.action_libraryFragment_to_genreCatalogueFragment)
        }
        binding.playlistCatalogueTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.PLAYLIST_ALL, Constants.PLAYLIST_ALL)
            }
            activity.navController.navigate(R.id.action_libraryFragment_to_playlistCatalogueFragment, bundle)
        }

        binding.albumCatalogueSampleTextViewRefreshable.setOnLongClickListener {
            libraryViewModel.refreshAlbumSample(viewLifecycleOwner)
            true
        }
        binding.artistCatalogueSampleTextViewRefreshable.setOnLongClickListener {
            libraryViewModel.refreshArtistSample(viewLifecycleOwner)
            true
        }
        binding.genreCatalogueSampleTextViewRefreshable.setOnLongClickListener {
            libraryViewModel.refreshGenreSample(viewLifecycleOwner)
            true
        }
        binding.playlistCatalogueSampleTextViewRefreshable.setOnLongClickListener {
            libraryViewModel.refreshPlaylistSample(viewLifecycleOwner)
            true
        }
    }

    private fun initAppBar() {
        materialToolbar = binding.root.findViewById(R.id.toolbar)

        activity.setSupportActionBar(materialToolbar)
        // Objects.requireNonNull(materialToolbar.getOverflowIcon()) implies non-null
        materialToolbar.overflowIcon!!.setTint(
            requireContext().resources.getColor(R.color.titleTextColor, null)
        )
    }

    private fun initMusicFolderView() {
        if (!Preferences.isMusicDirectorySectionVisible()) {
            binding.libraryMusicFolderSector.visibility = View.GONE
            return
        }

        binding.musicFolderRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = MusicFolderAdapter(this@LibraryFragment).also { musicFolderAdapter = it }
        }

        libraryViewModel.getMusicFolders(viewLifecycleOwner).observe(viewLifecycleOwner) { musicFolders ->
            _binding?.libraryMusicFolderSector?.let { sector ->
                val folders = musicFolders.orEmpty()
                sector.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
                if (folders.isNotEmpty()) {
                    musicFolderAdapter.setItems(folders)
                }
            }
        }
    }

    private fun initAlbumView() {
        binding.albumRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = AlbumAdapter(this@LibraryFragment).also { albumAdapter = it }
        }

        libraryViewModel.getAlbumSample(viewLifecycleOwner).observe(viewLifecycleOwner) { albums ->
            _binding?.libraryAlbumSector?.let { sector ->
                val items = albums.orEmpty()
                sector.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                if (items.isNotEmpty()) {
                    albumAdapter.setItems(items)
                }
            }
        }

        CustomLinearSnapHelper().attachToRecyclerView(binding.albumRecyclerView)
    }

    private fun initArtistView() {
        binding.artistRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = ArtistAdapter(this@LibraryFragment, false, false).also { artistAdapter = it }
        }

        libraryViewModel.getArtistSample(viewLifecycleOwner).observe(viewLifecycleOwner) { artists ->
            _binding?.libraryArtistSector?.let { sector ->
                val items = artists.orEmpty()
                sector.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                if (items.isNotEmpty()) {
                    artistAdapter.artists = items
                }
            }
        }

        CustomLinearSnapHelper().attachToRecyclerView(binding.artistRecyclerView)
    }

    private fun initGenreView() {
        binding.genreRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = GenreAdapter(this@LibraryFragment).also { genreAdapter = it }
        }

        libraryViewModel.getGenreSample(viewLifecycleOwner).observe(viewLifecycleOwner) { genres ->
            _binding?.libraryGenresSector?.let { sector ->
                val items = genres.orEmpty()
                sector.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                if (items.isNotEmpty()) {
                    genreAdapter.setItems(items)
                }
            }
        }

        CustomLinearSnapHelper().attachToRecyclerView(binding.genreRecyclerView)
    }

    private fun initPlaylistView() {
        binding.playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = PlaylistHorizontalAdapter(this@LibraryFragment).also { playlistHorizontalAdapter = it }
        }

        libraryViewModel.getPlaylistSample(viewLifecycleOwner).observe(viewLifecycleOwner) { playlists ->
            _binding?.libraryPlaylistSector?.let { sector ->
                val items = playlists.orEmpty()
                sector.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                if (items.isNotEmpty()) {
                    playlistHorizontalAdapter.setItems(items)
                }
            }
        }
    }

    private fun refreshPlaylistView() {
        val handler = Handler(Looper.getMainLooper())

        val runnable = Runnable {
            // Check _binding explicitly because it might be null after onDestroyView.
            // libraryViewModel is lateinit, check if initialized.
            if (_binding != null && ::libraryViewModel.isInitialized) {
                libraryViewModel.refreshPlaylistSample(viewLifecycleOwner)
            }
        }

        handler.postDelayed(runnable, 100)
    }

    override fun onAlbumClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    override fun onArtistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }

    override fun onGenreClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songListPageFragment, bundle)
    }

    override fun onPlaylistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.playlistPageFragment, bundle)
    }

    override fun onPlaylistLongClick(bundle: Bundle) {
        val dialog = PlaylistEditorDialog(object : PlaylistCallback {
            override fun onDismiss() {
                refreshPlaylistView()
            }
        })

        dialog.arguments = bundle
        dialog.show(activity.supportFragmentManager, null)
    }

    override fun onMusicFolderClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.indexFragment, bundle)
    }
}
