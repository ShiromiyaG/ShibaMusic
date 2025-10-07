@file:OptIn(UnstableApi::class)

package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.viewpager2.widget.ViewPager2
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentHomeTabMusicBinding
import com.shirou.shibamusic.helper.recyclerview.CustomLinearSnapHelper
import com.shirou.shibamusic.helper.recyclerview.DotsIndicatorDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.PlaylistCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumAdapter
import com.shirou.shibamusic.ui.adapter.AlbumHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.ArtistAdapter
import com.shirou.shibamusic.ui.adapter.ArtistHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.DiscoverSongAdapter
import com.shirou.shibamusic.ui.adapter.PlaylistHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.ShareHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.SimilarTrackAdapter
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.YearAdapter
import com.shirou.shibamusic.ui.dialog.HomeRearrangementDialog
import com.shirou.shibamusic.ui.dialog.PlaylistEditorDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture

class HomeTabMusicFragment : Fragment(), ClickCallback, PlaylistCallback {

    private val TAG = "HomeFragment"

    private var _binding: FragmentHomeTabMusicBinding? = null
    private val binding get() = _binding!!

    private lateinit var activityRef: MainActivity
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var discoverSongAdapter: DiscoverSongAdapter
    private lateinit var similarMusicAdapter: SimilarTrackAdapter
    private lateinit var radioArtistAdapter: ArtistHorizontalAdapter
    private lateinit var bestOfArtistAdapter: ArtistAdapter
    private lateinit var starredSongAdapter: SongHorizontalAdapter
    private lateinit var topSongAdapter: SongHorizontalAdapter
    private lateinit var starredAlbumAdapter: AlbumHorizontalAdapter
    private lateinit var starredArtistAdapter: ArtistHorizontalAdapter
    private lateinit var recentlyAddedAlbumAdapter: AlbumAdapter
    private lateinit var recentlyPlayedAlbumAdapter: AlbumAdapter
    private lateinit var mostPlayedAlbumAdapter: AlbumAdapter
    private lateinit var newReleasesAlbumAdapter: AlbumHorizontalAdapter
    private lateinit var yearAdapter: YearAdapter
    private lateinit var playlistHorizontalAdapter: PlaylistHorizontalAdapter
    private lateinit var shareHorizontalAdapter: ShareHorizontalAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    @Nullable
    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        activityRef = requireActivity() as MainActivity
        _binding = FragmentHomeTabMusicBinding.inflate(inflater, container, false)
        val view = binding.root
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        init()
        return view
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSyncStarredView()
        initSyncStarredAlbumsView()
        initDiscoverSongSlideView()
        initSimilarSongView()
        initArtistRadio()
        initArtistBestOf()
        initStarredTracksView()
        initStarredAlbumsView()
        initStarredArtistsView()
        initMostPlayedAlbumView()
        initRecentPlayedAlbumView()
        initNewReleasesView()
        initYearSongView()
        initRecentAddedAlbumView()
        initTopSongsView()
        initPinnedPlaylistsView()
        initSharesView()
        initHomeReorganizer()
        reorder()
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
    }

    override fun onResume() {
        super.onResume()
        refreshSharesView()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        binding.discoveryTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshDiscoverySongSample(viewLifecycleOwner)
            true
        }
        binding.discoveryTextViewClickable.setOnClickListener {
            homeViewModel.getRandomShuffleSample().observe(viewLifecycleOwner) { songs: List<Child>? ->
                val filtered = MusicUtil.ratingFilter(songs)
                if (filtered.isNotEmpty()) {
                    MediaManager.startQueue(mediaBrowserListenableFuture, filtered, 0)
                    activityRef.setBottomSheetInPeek(true)
                }
            }
        }
        binding.similarTracksTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshSimilarSongSample(viewLifecycleOwner)
            true
        }
        binding.radioArtistTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshRadioArtistSample(viewLifecycleOwner)
            true
        }
        binding.bestOfArtistTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshBestOfArtist(viewLifecycleOwner)
            true
        }
        binding.starredTracksTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.MEDIA_STARRED, Constants.MEDIA_STARRED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_songListPageFragment, bundle)
        }
        binding.starredAlbumsTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.ALBUM_STARRED, Constants.ALBUM_STARRED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_albumListPageFragment, bundle)
        }
        binding.starredArtistsTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.ARTIST_STARRED, Constants.ARTIST_STARRED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_artistListPageFragment, bundle)
        }
        binding.recentlyAddedAlbumsTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.ALBUM_RECENTLY_ADDED, Constants.ALBUM_RECENTLY_ADDED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_albumListPageFragment, bundle)
        }
        binding.recentlyPlayedAlbumsTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.ALBUM_RECENTLY_PLAYED, Constants.ALBUM_RECENTLY_PLAYED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_albumListPageFragment, bundle)
        }
        binding.mostPlayedAlbumsTextViewClickable.setOnClickListener {
            val bundle = Bundle().apply {
                putString(Constants.ALBUM_MOST_PLAYED, Constants.ALBUM_MOST_PLAYED)
            }
            activityRef.navigateIfPossible(R.id.action_homeFragment_to_albumListPageFragment, bundle)
        }
        binding.starredTracksTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshStarredTracks(viewLifecycleOwner)
            true
        }
        binding.starredAlbumsTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshStarredAlbums(viewLifecycleOwner)
            true
        }
        binding.starredArtistsTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshStarredArtists(viewLifecycleOwner)
            true
        }
        binding.recentlyPlayedAlbumsTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshRecentlyPlayedAlbumList(viewLifecycleOwner)
            true
        }
        binding.mostPlayedAlbumsTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshMostPlayedAlbums(viewLifecycleOwner)
            true
        }
        binding.recentlyAddedAlbumsTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshMostRecentlyAddedAlbums(viewLifecycleOwner)
            true
        }
        binding.sharesTextViewRefreshable.setOnLongClickListener {
            homeViewModel.refreshShares(viewLifecycleOwner)
            true
        }
        binding.gridTracksPreTextView.setOnClickListener { v ->
            showPopupMenu(v, R.menu.filter_top_songs_popup_menu)
        }
    }

    private fun initSyncStarredView() {
        if (Preferences.isStarredSyncEnabled()) {
            val starredTracksObserver = object : Observer<List<Child>?> {
                override fun onChanged(songs: List<Child>?) {
                    if (songs != null) {
                        val tracker = DownloadUtil.getDownloadTracker(requireContext())
                        val toSync = songs.filter { it.id != null && !tracker.isDownloaded(it.id!!) }
                            .mapNotNull { it.title }
                        if (toSync.isNotEmpty()) {
                            binding.homeSyncStarredCard.isVisible = true
                            binding.homeSyncStarredTracksToSync.text = toSync.joinToString()
                        }
                    }
                    homeViewModel.getAllStarredTracks().removeObserver(this)
                }
            }
            homeViewModel.getAllStarredTracks().observeForever(starredTracksObserver)
            binding.homeSyncStarredCancel.setOnClickListener {
                binding.homeSyncStarredCard.isVisible = false
            }
            binding.homeSyncStarredDownload.setOnClickListener {
                val downloadObserver = object : Observer<List<Child>?> {
                    override fun onChanged(list: List<Child>?) {
                        if (list != null) {
                            val tracker = DownloadUtil.getDownloadTracker(requireContext())
                            list.forEach { child ->
                                val id = child.id ?: return@forEach
                                if (!tracker.isDownloaded(id)) {
                                    tracker.download(MappingUtil.mapDownload(child), Download(id))
                                }
                            }
                        }
                        homeViewModel.getAllStarredTracks().removeObserver(this)
                        binding.homeSyncStarredCard.isVisible = false
                    }
                }
                homeViewModel.getAllStarredTracks().observeForever(downloadObserver)
            }
        }
    }

    private fun initSyncStarredAlbumsView() {
        if (Preferences.isStarredAlbumsSyncEnabled()) {
            val starredAlbumsObserver = object : Observer<List<AlbumID3>?> {
                override fun onChanged(albums: List<AlbumID3>?) {
                    if (albums != null) {
                        val count = albums.size
                        if (count > 0) {
                            binding.homeSyncStarredAlbumsCard.isVisible = true
                            binding.homeSyncStarredAlbumsToSync.text =
                                resources.getQuantityString(R.plurals.home_sync_starred_albums_count, count, count)
                        }
                    }
                    homeViewModel.getStarredAlbums(viewLifecycleOwner).removeObserver(this)
                }
            }
            homeViewModel.getStarredAlbums(viewLifecycleOwner).observeForever(starredAlbumsObserver)
            binding.homeSyncStarredAlbumsCancel.setOnClickListener {
                binding.homeSyncStarredAlbumsCard.isVisible = false
            }
            binding.homeSyncStarredAlbumsDownload.setOnClickListener {
                val allSongsObserver = object : Observer<List<Child>> {
                    override fun onChanged(allSongs: List<Child>) {
                        val tracker = DownloadUtil.getDownloadTracker(requireContext())
                        allSongs.forEach { child ->
                            val id = child.id ?: return@forEach
                            if (!tracker.isDownloaded(id)) {
                                tracker.download(MappingUtil.mapDownload(child), Download(id))
                            }
                        }
                        homeViewModel.getAllStarredAlbumSongs().removeObserver(this)
                        binding.homeSyncStarredAlbumsCard.isVisible = false
                    }
                }
                homeViewModel.getAllStarredAlbumSongs().observeForever(allSongsObserver)
            }
        }
    }

    private fun initDiscoverSongSlideView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_DISCOVERY)) return
        binding.discoverSongViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        discoverSongAdapter = DiscoverSongAdapter(this)
        binding.discoverSongViewPager.adapter = discoverSongAdapter
        binding.discoverSongViewPager.offscreenPageLimit = 1
        homeViewModel.getDiscoverSongSample(viewLifecycleOwner).observe(viewLifecycleOwner) { songs: List<Child>? ->
            val filtered = MusicUtil.ratingFilter(songs)
            binding.homeDiscoverSector.isVisible = filtered.isNotEmpty()
            discoverSongAdapter.setItems(filtered)
        }
        setSlideViewOffset(binding.discoverSongViewPager, 20f, 16f)
    }

    private fun initSimilarSongView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_MADE_FOR_YOU)) return
        binding.similarTracksRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.similarTracksRecyclerView.setHasFixedSize(true)
        similarMusicAdapter = SimilarTrackAdapter(this)
        binding.similarTracksRecyclerView.adapter = similarMusicAdapter
        homeViewModel.getStarredTracksSample(viewLifecycleOwner).observe(viewLifecycleOwner) { songs: List<Child>? ->
            val filtered = MusicUtil.ratingFilter(songs)
            binding.homeSimilarTracksSector.isVisible = filtered.isNotEmpty()
            similarMusicAdapter.setItems(filtered)
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.similarTracksRecyclerView)
    }

    private fun initArtistBestOf() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_BEST_OF)) return
        binding.bestOfArtistRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.bestOfArtistRecyclerView.setHasFixedSize(true)
        bestOfArtistAdapter = ArtistAdapter(this, false, true)
        binding.bestOfArtistRecyclerView.adapter = bestOfArtistAdapter
        homeViewModel.getBestOfArtists(viewLifecycleOwner).observe(viewLifecycleOwner) { artists: List<ArtistID3>? ->
            binding.homeBestOfArtistSector.isVisible = artists?.isNotEmpty() == true
            bestOfArtistAdapter.artists = artists.orEmpty()
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.bestOfArtistRecyclerView)
    }

    private fun initArtistRadio() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_RADIO_STATION)) return
        binding.radioArtistRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.radioArtistRecyclerView.setHasFixedSize(true)
        radioArtistAdapter = ArtistHorizontalAdapter(this)
        binding.radioArtistRecyclerView.adapter = radioArtistAdapter
        homeViewModel.getStarredArtistsSample(viewLifecycleOwner).observe(viewLifecycleOwner) { artists: List<ArtistID3>? ->
            binding.homeRadioArtistSector.isVisible = artists?.isNotEmpty() == true
            binding.afterRadioArtistDivider.isVisible = artists?.isNotEmpty() == true
            radioArtistAdapter.setItems(artists.orEmpty())
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.radioArtistRecyclerView)
    }

    private fun initTopSongsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_TOP_SONGS)) return
        binding.topSongsRecyclerView.setHasFixedSize(true)
        topSongAdapter = SongHorizontalAdapter(this, true, false, null)
        binding.topSongsRecyclerView.adapter = topSongAdapter
        homeViewModel.getChronologySample(viewLifecycleOwner).observe(viewLifecycleOwner) { chronologies ->
            binding.apply {
                if (chronologies.isNullOrEmpty()) {
                    homeGridTracksSector.isVisible = false
                    afterGridDivider.isVisible = false
                } else {
                    homeGridTracksSector.isVisible = true
                    afterGridDivider.isVisible = true
                    topSongsRecyclerView.layoutManager = GridLayoutManager(
                        requireContext(),
                        UIUtil.getSpanCount(chronologies.size, 5),
                        GridLayoutManager.HORIZONTAL,
                        false
                    )
                    val topSongs: List<Child> = chronologies.map { it as Child }
                    topSongAdapter.setItems(topSongs)
                }
            }
            PagerSnapHelper().attachToRecyclerView(binding.topSongsRecyclerView)
            binding.topSongsRecyclerView.addItemDecoration(
                DotsIndicatorDecoration(
                    resources.getDimensionPixelSize(R.dimen.radius),
                    resources.getDimensionPixelSize(R.dimen.radius) * 4,
                    resources.getDimensionPixelSize(R.dimen.dots_height),
                    ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                    ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                )
            )
        }
    }

    private fun initStarredTracksView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_TRACKS)) return
        binding.starredTracksRecyclerView.setHasFixedSize(true)
        starredSongAdapter = SongHorizontalAdapter(this, true, false, null)
        binding.starredTracksRecyclerView.adapter = starredSongAdapter
        homeViewModel.getStarredTracks(viewLifecycleOwner).observe(viewLifecycleOwner) { songs: List<Child>? ->
            binding.apply {
                starredTracksSector.isVisible = songs?.isNotEmpty() == true
                starredTracksRecyclerView.layoutManager = GridLayoutManager(
                    requireContext(),
                    UIUtil.getSpanCount(songs?.size ?: 0, 5),
                    GridLayoutManager.HORIZONTAL,
                    false
                )
            }
            starredSongAdapter.setItems(songs.orEmpty())
            PagerSnapHelper().attachToRecyclerView(binding.starredTracksRecyclerView)
            binding.starredTracksRecyclerView.addItemDecoration(
                DotsIndicatorDecoration(
                    resources.getDimensionPixelSize(R.dimen.radius),
                    resources.getDimensionPixelSize(R.dimen.radius) * 4,
                    resources.getDimensionPixelSize(R.dimen.dots_height),
                    ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                    ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                )
            )
        }
    }

    private fun initStarredAlbumsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ALBUMS)) return
        binding.starredAlbumsRecyclerView.setHasFixedSize(true)
        starredAlbumAdapter = AlbumHorizontalAdapter(this, false)
        binding.starredAlbumsRecyclerView.adapter = starredAlbumAdapter
        homeViewModel.getStarredAlbums(viewLifecycleOwner).observe(viewLifecycleOwner) { albums: List<AlbumID3>? ->
            binding.apply {
                starredAlbumsSector.isVisible = albums?.isNotEmpty() == true
                starredAlbumsRecyclerView.layoutManager = GridLayoutManager(
                    requireContext(),
                    UIUtil.getSpanCount(albums?.size ?: 0, 5),
                    GridLayoutManager.HORIZONTAL,
                    false
                )
            }
            starredAlbumAdapter.setItems(albums.orEmpty())
            PagerSnapHelper().attachToRecyclerView(binding.starredAlbumsRecyclerView)
            binding.starredAlbumsRecyclerView.addItemDecoration(
                DotsIndicatorDecoration(
                    resources.getDimensionPixelSize(R.dimen.radius),
                    resources.getDimensionPixelSize(R.dimen.radius) * 4,
                    resources.getDimensionPixelSize(R.dimen.dots_height),
                    ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                    ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                )
            )
        }
    }

    private fun initStarredArtistsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ARTISTS)) return
        binding.starredArtistsRecyclerView.setHasFixedSize(true)
        starredArtistAdapter = ArtistHorizontalAdapter(this)
        binding.starredArtistsRecyclerView.adapter = starredArtistAdapter
        homeViewModel.getStarredArtists(viewLifecycleOwner).observe(viewLifecycleOwner) { artists: List<ArtistID3>? ->
            binding.apply {
                starredArtistsSector.isVisible = artists?.isNotEmpty() == true
                afterFavoritesDivider.isVisible = artists?.isNotEmpty() == true
                starredArtistsRecyclerView.layoutManager = GridLayoutManager(
                    requireContext(),
                    UIUtil.getSpanCount(artists?.size ?: 0, 5),
                    GridLayoutManager.HORIZONTAL,
                    false
                )
            }
            starredArtistAdapter.setItems(artists.orEmpty())
            PagerSnapHelper().attachToRecyclerView(binding.starredArtistsRecyclerView)
            binding.starredArtistsRecyclerView.addItemDecoration(
                DotsIndicatorDecoration(
                    resources.getDimensionPixelSize(R.dimen.radius),
                    resources.getDimensionPixelSize(R.dimen.radius) * 4,
                    resources.getDimensionPixelSize(R.dimen.dots_height),
                    ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                    ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                )
            )
        }
    }

    private fun initNewReleasesView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_NEW_RELEASES)) return
        binding.newReleasesRecyclerView.setHasFixedSize(true)
        newReleasesAlbumAdapter = AlbumHorizontalAdapter(this, false)
        binding.newReleasesRecyclerView.adapter = newReleasesAlbumAdapter
        homeViewModel.getRecentlyReleasedAlbums(viewLifecycleOwner).observe(viewLifecycleOwner) { albums: List<AlbumID3>? ->
            binding.apply {
                homeNewReleasesSector.isVisible = albums?.isNotEmpty() == true
                newReleasesRecyclerView.layoutManager = GridLayoutManager(
                    requireContext(),
                    UIUtil.getSpanCount(albums?.size ?: 0, 5),
                    GridLayoutManager.HORIZONTAL,
                    false
                )
            }
            newReleasesAlbumAdapter.setItems(albums.orEmpty())
            PagerSnapHelper().attachToRecyclerView(binding.newReleasesRecyclerView)
            binding.newReleasesRecyclerView.addItemDecoration(
                DotsIndicatorDecoration(
                    resources.getDimensionPixelSize(R.dimen.radius),
                    resources.getDimensionPixelSize(R.dimen.radius) * 4,
                    resources.getDimensionPixelSize(R.dimen.dots_height),
                    ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                    ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                )
            )
        }
    }

    private fun initYearSongView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_FLASHBACK)) return
        binding.yearsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.yearsRecyclerView.setHasFixedSize(true)
        yearAdapter = YearAdapter(this)
        binding.yearsRecyclerView.adapter = yearAdapter
        homeViewModel.getYearList(viewLifecycleOwner).observe(viewLifecycleOwner) { years: List<Int>? ->
            binding.homeFlashbackSector.isVisible = years?.isNotEmpty() == true
            yearAdapter.setItems(years.orEmpty())
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.yearsRecyclerView)
    }

    private fun initMostPlayedAlbumView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_MOST_PLAYED)) return
        binding.mostPlayedAlbumsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.mostPlayedAlbumsRecyclerView.setHasFixedSize(true)
        mostPlayedAlbumAdapter = AlbumAdapter(this)
        binding.mostPlayedAlbumsRecyclerView.adapter = mostPlayedAlbumAdapter
        homeViewModel.getMostPlayedAlbums(viewLifecycleOwner).observe(viewLifecycleOwner) { albums: List<AlbumID3>? ->
            binding.homeMostPlayedAlbumsSector.isVisible = albums?.isNotEmpty() == true
            mostPlayedAlbumAdapter.setItems(albums.orEmpty())
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.mostPlayedAlbumsRecyclerView)
    }

    private fun initRecentPlayedAlbumView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_LAST_PLAYED)) return
        binding.recentlyPlayedAlbumsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyPlayedAlbumsRecyclerView.setHasFixedSize(true)
        recentlyPlayedAlbumAdapter = AlbumAdapter(this)
        binding.recentlyPlayedAlbumsRecyclerView.adapter = recentlyPlayedAlbumAdapter
        homeViewModel.getRecentlyPlayedAlbumList(viewLifecycleOwner).observe(viewLifecycleOwner) { albums: List<AlbumID3>? ->
            binding.homeRecentlyPlayedAlbumsSector.isVisible = albums?.isNotEmpty() == true
            recentlyPlayedAlbumAdapter.setItems(albums.orEmpty())
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.recentlyPlayedAlbumsRecyclerView)
    }

    private fun initRecentAddedAlbumView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_RECENTLY_ADDED)) return
        binding.recentlyAddedAlbumsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyAddedAlbumsRecyclerView.setHasFixedSize(true)
        recentlyAddedAlbumAdapter = AlbumAdapter(this)
        binding.recentlyAddedAlbumsRecyclerView.adapter = recentlyAddedAlbumAdapter
        homeViewModel.getMostRecentlyAddedAlbums(viewLifecycleOwner).observe(viewLifecycleOwner) { albums: List<AlbumID3>? ->
            binding.homeRecentlyAddedAlbumsSector.isVisible = albums?.isNotEmpty() == true
            recentlyAddedAlbumAdapter.setItems(albums.orEmpty())
        }
        CustomLinearSnapHelper().attachToRecyclerView(binding.recentlyAddedAlbumsRecyclerView)
    }

    private fun initPinnedPlaylistsView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_PINNED_PLAYLISTS)) return
        binding.pinnedPlaylistsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.pinnedPlaylistsRecyclerView.setHasFixedSize(true)
        playlistHorizontalAdapter = PlaylistHorizontalAdapter(this)
        binding.pinnedPlaylistsRecyclerView.adapter = playlistHorizontalAdapter
        homeViewModel.getPinnedPlaylists(viewLifecycleOwner).observe(viewLifecycleOwner) { playlists ->
            binding.pinnedPlaylistsSector.isVisible = playlists?.isNotEmpty() == true
            playlistHorizontalAdapter.setItems(playlists.orEmpty())
        }
    }

    private fun initSharesView() {
        if (homeViewModel.checkHomeSectorVisibility(Constants.HOME_SECTOR_SHARED)) return
        binding.sharesRecyclerView.setHasFixedSize(true)
        shareHorizontalAdapter = ShareHorizontalAdapter(this)
        binding.sharesRecyclerView.adapter = shareHorizontalAdapter
        if (Preferences.isSharingEnabled()) {
            homeViewModel.getShares(viewLifecycleOwner).observe(viewLifecycleOwner) { shares: List<Share>? ->
                binding.apply {
                    sharesSector.isVisible = shares?.isNotEmpty() == true
                    sharesRecyclerView.layoutManager = GridLayoutManager(
                        requireContext(),
                        UIUtil.getSpanCount(shares?.size ?: 0, 10)
                    )
                }
                shareHorizontalAdapter.setItems(shares.orEmpty())
                PagerSnapHelper().attachToRecyclerView(binding.sharesRecyclerView)
                binding.sharesRecyclerView.addItemDecoration(
                    DotsIndicatorDecoration(
                        resources.getDimensionPixelSize(R.dimen.radius),
                        resources.getDimensionPixelSize(R.dimen.radius) * 4,
                        resources.getDimensionPixelSize(R.dimen.dots_height),
                        ContextCompat.getColor(requireContext(), R.color.subtitleTextColor),
                        ContextCompat.getColor(requireContext(), R.color.titleTextColor)
                    )
                )
            }
        }
    }

    private fun initHomeReorganizer() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { binding.homeSectorRearrangementButton.isVisible = true }
        handler.postDelayed(runnable, 5000)
        binding.homeSectorRearrangementButton.setOnClickListener {
            HomeRearrangementDialog().show(requireActivity().supportFragmentManager, null)
        }
    }

    private fun refreshSharesView() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            if (view != null && _binding != null && Preferences.isSharingEnabled()) {
                homeViewModel.refreshShares(viewLifecycleOwner)
            }
        }
        handler.postDelayed(runnable, 100)
    }

    private fun setSlideViewOffset(viewPager: ViewPager2, pageOffset: Float, pageMargin: Float) {
        viewPager.setPageTransformer { page, position ->
            val myOffset = position * -(2 * pageOffset + pageMargin)
            if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    page.translationX = -myOffset
                } else {
                    page.translationX = myOffset
                }
            } else {
                page.translationY = myOffset
            }
        }
    }

    fun reorder() {
        _binding?.let { binding ->
            homeViewModel.getHomeSectorList()?.let { sectors ->
                binding.homeLinearLayoutContainer.removeAllViews()
                for (sector in sectors) {
                    if (!sector.isVisible) continue
                    when (sector.id) {
                        Constants.HOME_SECTOR_DISCOVERY -> binding.homeLinearLayoutContainer.addView(binding.homeDiscoverSector)
                        Constants.HOME_SECTOR_MADE_FOR_YOU -> binding.homeLinearLayoutContainer.addView(binding.homeSimilarTracksSector)
                        Constants.HOME_SECTOR_BEST_OF -> binding.homeLinearLayoutContainer.addView(binding.homeBestOfArtistSector)
                        Constants.HOME_SECTOR_RADIO_STATION -> binding.homeLinearLayoutContainer.addView(binding.homeRadioArtistSector)
                        Constants.HOME_SECTOR_TOP_SONGS -> binding.homeLinearLayoutContainer.addView(binding.homeGridTracksSector)
                        Constants.HOME_SECTOR_STARRED_TRACKS -> binding.homeLinearLayoutContainer.addView(binding.starredTracksSector)
                        Constants.HOME_SECTOR_STARRED_ALBUMS -> binding.homeLinearLayoutContainer.addView(binding.starredAlbumsSector)
                        Constants.HOME_SECTOR_STARRED_ARTISTS -> binding.homeLinearLayoutContainer.addView(binding.starredArtistsSector)
                        Constants.HOME_SECTOR_NEW_RELEASES -> binding.homeLinearLayoutContainer.addView(binding.homeNewReleasesSector)
                        Constants.HOME_SECTOR_FLASHBACK -> binding.homeLinearLayoutContainer.addView(binding.homeFlashbackSector)
                        Constants.HOME_SECTOR_MOST_PLAYED -> binding.homeLinearLayoutContainer.addView(binding.homeMostPlayedAlbumsSector)
                        Constants.HOME_SECTOR_LAST_PLAYED -> binding.homeLinearLayoutContainer.addView(binding.homeRecentlyPlayedAlbumsSector)
                        Constants.HOME_SECTOR_RECENTLY_ADDED -> binding.homeLinearLayoutContainer.addView(binding.homeRecentlyAddedAlbumsSector)
                        Constants.HOME_SECTOR_PINNED_PLAYLISTS -> binding.homeLinearLayoutContainer.addView(binding.pinnedPlaylistsSector)
                        Constants.HOME_SECTOR_SHARED -> binding.homeLinearLayoutContainer.addView(binding.sharesSector)
                    }
                }
                binding.homeLinearLayoutContainer.addView(binding.homeSectorRearrangementButton)
            }
        }
    }

    private fun showPopupMenu(view: View, menuResource: Int) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(menuResource, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_last_week_name -> {
                        homeViewModel.changeChronologyPeriod(viewLifecycleOwner, 0)
                        binding.gridTracksPreTextView.text = getString(R.string.home_title_last_week)
                        true
                    }
                    R.id.menu_last_month_name -> {
                        homeViewModel.changeChronologyPeriod(viewLifecycleOwner, 1)
                        binding.gridTracksPreTextView.text = getString(R.string.home_title_last_month)
                        true
                    }
                    R.id.menu_last_year_name -> {
                        homeViewModel.changeChronologyPeriod(viewLifecycleOwner, 2)
                        binding.gridTracksPreTextView.text = getString(R.string.home_title_last_year)
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun refreshPlaylistView() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            if (view != null && _binding != null) {
                homeViewModel.getPinnedPlaylists(viewLifecycleOwner)
            }
        }
        handler.postDelayed(runnable, 100)
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
        if (bundle.containsKey(Constants.MEDIA_MIX)) {
            val trackObject: Child? = if (Build.VERSION.SDK_INT >= 33) {
                BundleCompat.getParcelable(bundle, Constants.TRACK_OBJECT, Child::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable(Constants.TRACK_OBJECT)
            }
            trackObject?.let { track ->
                MediaManager.startQueue(mediaBrowserListenableFuture, listOf(track), 0)
                activityRef.setBottomSheetInPeek(true)
                homeViewModel.getMediaInstantMix(viewLifecycleOwner, track).observe(viewLifecycleOwner) { songs ->
                    val filtered = MusicUtil.ratingFilter(songs)
                    if (!filtered.isNullOrEmpty()) {
                        MediaManager.enqueue(mediaBrowserListenableFuture, filtered, true)
                    }
                }
            }
        } else if (bundle.containsKey(Constants.MEDIA_CHRONOLOGY)) {
            val media: ArrayList<Child>? = if (Build.VERSION.SDK_INT >= 33) {
                BundleCompat.getParcelableArrayList(bundle, Constants.TRACKS_OBJECT, Child::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelableArrayList(Constants.TRACKS_OBJECT)
            }
            MediaManager.startQueue(mediaBrowserListenableFuture, media.orEmpty(), bundle.getInt(Constants.ITEM_POSITION))
            activityRef.setBottomSheetInPeek(true)
        } else {
            val media: ArrayList<Child>? = if (Build.VERSION.SDK_INT >= 33) {
                BundleCompat.getParcelableArrayList(bundle, Constants.TRACKS_OBJECT, Child::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelableArrayList(Constants.TRACKS_OBJECT)
            }
            MediaManager.startQueue(mediaBrowserListenableFuture, media.orEmpty(), bundle.getInt(Constants.ITEM_POSITION))
            activityRef.setBottomSheetInPeek(true)
        }
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
        if (bundle.containsKey(Constants.MEDIA_MIX)) {
            Snackbar.make(requireView(), R.string.artist_adapter_radio_station_starting, Snackbar.LENGTH_LONG)
                .setAnchorView(activityRef.findViewById(R.id.player_bottom_sheet))
                .show()
            val artistObject: ArtistID3? = if (Build.VERSION.SDK_INT >= 33) {
                BundleCompat.getParcelable(bundle, Constants.ARTIST_OBJECT, ArtistID3::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable(Constants.ARTIST_OBJECT)
            }
            artistObject?.let { artist ->
                homeViewModel.getArtistInstantMix(viewLifecycleOwner, artist).observe(viewLifecycleOwner) { songs ->
                    val filtered = MusicUtil.ratingFilter(songs)
                    if (!filtered.isNullOrEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, filtered, 0)
                        activityRef.setBottomSheetInPeek(true)
                    }
                }
            }
        } else if (bundle.containsKey(Constants.MEDIA_BEST_OF)) {
            val artistObject: ArtistID3? = if (Build.VERSION.SDK_INT >= 33) {
                BundleCompat.getParcelable(bundle, Constants.ARTIST_OBJECT, ArtistID3::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable(Constants.ARTIST_OBJECT)
            }
            artistObject?.let { artist ->
                homeViewModel.getArtistBestOf(viewLifecycleOwner, artist).observe(viewLifecycleOwner) { songs ->
                    val filtered = MusicUtil.ratingFilter(songs)
                    if (!filtered.isNullOrEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, filtered, 0)
                        activityRef.setBottomSheetInPeek(true)
                    }
                }
            }
        } else {
            Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
        }

    }

    override fun onArtistLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }

    override fun onYearClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songListPageFragment, bundle)
    }

    override fun onShareClick(bundle: Bundle) {
        val share: Share? = if (Build.VERSION.SDK_INT >= 33) {
            BundleCompat.getParcelable(bundle, Constants.SHARE_OBJECT, Share::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(Constants.SHARE_OBJECT)
        }
        share?.url?.let {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    override fun onPlaylistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.playlistPageFragment, bundle)
    }

    override fun onPlaylistLongClick(bundle: Bundle) {
        val dialog = PlaylistEditorDialog(this).apply { arguments = bundle }
        dialog.setTargetFragment(this, 0)
        dialog.show(activityRef.supportFragmentManager, null)
    }
}
