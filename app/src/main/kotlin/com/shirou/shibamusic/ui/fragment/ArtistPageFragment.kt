package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentArtistPageBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.helper.recyclerview.CustomLinearSnapHelper
import com.shirou.shibamusic.helper.recyclerview.GridItemDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumCatalogueAdapter
import com.shirou.shibamusic.ui.adapter.ArtistCatalogueAdapter
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.viewmodel.ArtistPageViewModel
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class ArtistPageFragment : Fragment(), ClickCallback {
    private var _bind: FragmentArtistPageBinding? = null
    private val bind get() = _bind!!

    private lateinit var activity: MainActivity
    private lateinit var artistPageViewModel: ArtistPageViewModel

    private lateinit var songHorizontalAdapter: SongHorizontalAdapter
    private lateinit var albumCatalogueAdapter: AlbumCatalogueAdapter
    private lateinit var artistCatalogueAdapter: ArtistCatalogueAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(
        @NonNull inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _bind = FragmentArtistPageBinding.inflate(inflater, container, false)
        val view = bind.root
        artistPageViewModel = ViewModelProvider(requireActivity())[ArtistPageViewModel::class.java]

        init()
        initAppBar()
        initArtistInfo()
        initPlayButtons()
        initTopSongsView()
        initAlbumsView()
        initSimilarArtistsView()

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
        _bind = null
    }

    private fun init() {
        @Suppress("DEPRECATION")
        val artistArgument: ArtistID3? = arguments?.getParcelable(Constants.ARTIST_OBJECT)
        artistPageViewModel.artist = artistArgument

        if (artistArgument == null) {
            Toast.makeText(requireContext(), getString(R.string.artist_error_retrieving_artist), Toast.LENGTH_SHORT).show()
            activity.navigateUpIfPossible()
            return
        }

        bind.mostStreamedSongTextViewClickable.setOnClickListener {
            artistPageViewModel.artist?.let { artist ->
                val bundle = Bundle().apply {
                    putString(Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_ARTIST)
                    putParcelable(Constants.ARTIST_OBJECT, artist)
                }
                activity.navigateIfPossible(R.id.action_artistPageFragment_to_songListPageFragment, bundle)
            } ?: Toast.makeText(requireContext(), getString(R.string.artist_error_retrieving_tracks), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(bind.animToolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

    bind.collapsingToolbar.title = artistPageViewModel.artist?.name.orEmpty()
    bind.animToolbar.setNavigationOnClickListener { activity.navigateUpIfPossible() }
        bind.collapsingToolbar.setExpandedTitleColor(resources.getColor(R.color.white, null))
    }

    private fun initArtistInfo() {
        val artistId = artistPageViewModel.artist?.id

        if (artistId.isNullOrEmpty()) {
            _bind?.artistPageBioSector?.visibility = View.GONE
            return
        }

        artistPageViewModel.getArtistInfo(artistId).observe(viewLifecycleOwner) { artistInfo ->
            _bind?.let { binding ->
                if (artistInfo == null) {
                    binding.artistPageBioSector.visibility = View.GONE
                } else {
                    val normalizedBio = MusicUtil.forceReadableString(artistInfo.biography)

                    // Replicating Java's exact semantic for visibility, even if it looks like a potential bug
                    // where the final line overrides previous visibility setting based on normalizedBio.
                    binding.artistPageBioSector.visibility = if (normalizedBio.trim().isNotEmpty()) View.VISIBLE else View.GONE
                    binding.bioMoreTextViewClickable.visibility = if (artistInfo.lastFmUrl != null) View.VISIBLE else View.GONE

            context?.let { ctx ->
                        CustomGlideRequest.Builder
                .from(ctx, artistPageViewModel.artist?.id, CustomGlideRequest.ResourceType.Artist)
                            .build()
                            .into(binding.artistBackdropImageView)
                    }

                    binding.bioTextView.text = normalizedBio

                    binding.bioMoreTextViewClickable.setOnClickListener {
                        artistInfo.lastFmUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        }
                    }

                    // This line explicitly sets visibility to VISIBLE if artistInfo is not null,
                    // potentially overriding the previous conditional setting based on normalizedBio.
                    binding.artistPageBioSector.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initPlayButtons() {
        bind.artistPageShuffleButton.setOnClickListener {
            artistPageViewModel.getArtistShuffleList().observe(viewLifecycleOwner) { songs ->
                if (songs.isNotEmpty()) {
                    MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                    // // activity.isBottomSheetInPeek = true
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.artist_error_retrieving_tracks),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        bind.artistPageRadioButton.setOnClickListener {
            artistPageViewModel.getArtistInstantMix().observe(viewLifecycleOwner) { songs ->
                if (songs.isNotEmpty()) {
                    MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                    // // activity.isBottomSheetInPeek = true
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.artist_error_retrieving_radio),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun initTopSongsView() {
        bind.mostStreamedSongRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        songHorizontalAdapter = SongHorizontalAdapter(this, true, true, null)
        bind.mostStreamedSongRecyclerView.adapter = songHorizontalAdapter
        artistPageViewModel.getArtistTopSongList().observe(viewLifecycleOwner) { songs ->
            _bind?.let { binding ->
                if (songs == null) {
                    binding.artistPageTopSongsSector.visibility = View.GONE
                } else {
                    val isSongsListNotEmpty = songs.isNotEmpty()
                    binding.artistPageTopSongsSector.visibility = if (isSongsListNotEmpty) View.VISIBLE else View.GONE
                    binding.artistPageShuffleButton.isEnabled = isSongsListNotEmpty
                    songHorizontalAdapter.setItems(songs)
                }
            }
        }
    }

    private fun initAlbumsView() {
        bind.albumsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        bind.albumsRecyclerView.addItemDecoration(GridItemDecoration(2, 20, false))
        bind.albumsRecyclerView.setHasFixedSize(true)

        albumCatalogueAdapter = AlbumCatalogueAdapter(this, false)
        bind.albumsRecyclerView.adapter = albumCatalogueAdapter

        artistPageViewModel.getAlbumList().observe(viewLifecycleOwner) { albums ->
            _bind?.let { binding ->
                if (albums == null) {
                    binding.artistPageAlbumsSector.visibility = View.GONE
                } else {
                    binding.artistPageAlbumsSector.visibility = if (albums.isNotEmpty()) View.VISIBLE else View.GONE
                    albumCatalogueAdapter.setItems(albums)
                }
            }
        }
    }

    private fun initSimilarArtistsView() {
        bind.similarArtistsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        bind.similarArtistsRecyclerView.addItemDecoration(GridItemDecoration(2, 20, false))
        bind.similarArtistsRecyclerView.setHasFixedSize(true)

        artistCatalogueAdapter = ArtistCatalogueAdapter(this)
        bind.similarArtistsRecyclerView.adapter = artistCatalogueAdapter

        artistPageViewModel.getArtistInfo(artistPageViewModel.artist?.id).observe(viewLifecycleOwner) { artist ->
            _bind?.let { binding ->
                if (artist == null) {
                    binding.similarArtistSector.visibility = View.GONE
                } else {
                    // SimilarArtists can be null, check for null before isEmpty()
                    binding.similarArtistSector.visibility = if (artist.similarArtists?.isNotEmpty() == true) View.VISIBLE else View.GONE

                    val artists = mutableListOf<ArtistID3>()
                    artist.similarArtists?.let {
                        artists.addAll(it)
                    }

                    artistCatalogueAdapter.setItems(artists)
                }
            }
        }

        val similarArtistSnapHelper = CustomLinearSnapHelper()
        similarArtistSnapHelper.attachToRecyclerView(bind.similarArtistsRecyclerView)
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
    MediaManager.startQueue(mediaBrowserListenableFuture, tracks ?: emptyList(), position)
        // // activity.isBottomSheetInPeek = true
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
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
}
