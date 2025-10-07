package com.shirou.shibamusic.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.annotation.NonNull
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentSongListPageBinding
import com.shirou.shibamusic.helper.recyclerview.PaginationScrollListener
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Genre
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.SongListPageViewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.min

@UnstableApi
class SongListPageFragment : Fragment(), ClickCallback {
    private val TAG = "SongListPageFragment"

    private var bind: FragmentSongListPageBinding? = null
    private lateinit var activity: MainActivity
    private lateinit var songListPageViewModel: SongListPageViewModel
    private var songListLiveData: LiveData<List<Child>>? = null

    private lateinit var songHorizontalAdapter: SongHorizontalAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        bind = FragmentSongListPageBinding.inflate(inflater, container, false)
        val view = bind!!.root
        songListPageViewModel = ViewModelProvider(requireActivity())[SongListPageViewModel::class.java]

        init()
        initAppBar()
        initButtons()
        initSongListView()

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
        bind = null
    }

    private fun init() {
        val args = requireArguments()
        bind?.apply {
            when {
                args.getString(Constants.MEDIA_RECENTLY_PLAYED) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_RECENTLY_PLAYED
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_recently_played)
                    pageTitleLabel.setText(R.string.song_list_page_recently_played)
                }
                args.getString(Constants.MEDIA_MOST_PLAYED) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_MOST_PLAYED
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_most_played)
                    pageTitleLabel.setText(R.string.song_list_page_most_played)
                }
                args.getString(Constants.MEDIA_RECENTLY_ADDED) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_RECENTLY_ADDED
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_recently_added)
                    pageTitleLabel.setText(R.string.song_list_page_recently_added)
                }
                args.getString(Constants.MEDIA_BY_GENRE) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_BY_GENRE
                    songListPageViewModel.genre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        BundleCompat.getParcelable(args, Constants.GENRE_OBJECT, Genre::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelable(Constants.GENRE_OBJECT)
                    }
                    songListPageViewModel.genre?.let { genre ->
                        songListPageViewModel.toolbarTitle = genre.genre
                        pageTitleLabel.text = genre.genre
                    }
                }
                args.getString(Constants.MEDIA_BY_ARTIST) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_BY_ARTIST
                    songListPageViewModel.artist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        BundleCompat.getParcelable(args, Constants.ARTIST_OBJECT, ArtistID3::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelable(Constants.ARTIST_OBJECT)
                    }
                    songListPageViewModel.artist?.let { artist ->
                        songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_top, artist.name)
                        pageTitleLabel.text = getString(R.string.song_list_page_top, artist.name)
                    }
                }
                args.getString(Constants.MEDIA_BY_GENRES) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_BY_GENRES
                    songListPageViewModel.setFilters(args.getStringArrayList("filters_list"))
                    songListPageViewModel.setFilterNames(args.getStringArrayList("filter_name_list"))
                    songListPageViewModel.toolbarTitle = songListPageViewModel.getFiltersTitle()
                    pageTitleLabel.text = songListPageViewModel.getFiltersTitle()
                }
                args.getString(Constants.MEDIA_BY_YEAR) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_BY_YEAR
                    songListPageViewModel.year = args.getInt("year_object")
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_year, songListPageViewModel.year)
                    pageTitleLabel.text = getString(R.string.song_list_page_year, songListPageViewModel.year)
                }
                args.getString(Constants.MEDIA_STARRED) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_STARRED
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_starred)
                    pageTitleLabel.setText(R.string.song_list_page_starred)
                }
                args.getString(Constants.MEDIA_DOWNLOADED) != null -> {
                    songListPageViewModel.title = Constants.MEDIA_DOWNLOADED
                    songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_downloaded)
                    pageTitleLabel.setText(R.string.song_list_page_downloaded)
                }
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    BundleCompat.getParcelable(args, Constants.ALBUM_OBJECT, AlbumID3::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    args.getParcelable(Constants.ALBUM_OBJECT)
                }) != null -> {
                    songListPageViewModel.album = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        BundleCompat.getParcelable(args, Constants.ALBUM_OBJECT, AlbumID3::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        args.getParcelable(Constants.ALBUM_OBJECT)
                    }
                    songListPageViewModel.album?.let { album ->
                        songListPageViewModel.title = Constants.MEDIA_FROM_ALBUM
                        songListPageViewModel.toolbarTitle = album.name
                        pageTitleLabel.text = album.name
                    }
                }
            }
        }

        songListLiveData = songListPageViewModel.getSongList()
    }

    private fun initAppBar() {
        bind?.toolbar?.let { toolbar ->
            activity.setSupportActionBar(toolbar)

            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            toolbar.setNavigationOnClickListener { v ->
                hideKeyboard(v)
                activity.navController.navigateUp()
            }
        }

        bind?.appBarLayout?.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            bind?.apply {
                if ((albumInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(toolbar))) {
                    toolbar.title = songListPageViewModel.toolbarTitle
                } else {
                    toolbar.setTitle(R.string.empty_string)
                }
            }
        }
    }

    private fun initButtons() {
        songListLiveData?.observe(viewLifecycleOwner) { songs ->
            bind?.apply {
                setSongListPageSorter()

                songListShuffleImageView.setOnClickListener {
                    if (songs.isNotEmpty()) {
                        val shuffled = songs.shuffled()
                        val queue = shuffled.take(min(25, shuffled.size))
                        if (queue.isNotEmpty()) {
                            MediaManager.startQueue(mediaBrowserListenableFuture, queue, 0)
                            activity.setBottomSheetInPeek(true)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSongListView() {
        bind?.songListRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)

            songHorizontalAdapter = SongHorizontalAdapter(this@SongListPageFragment, true, false, null)
            adapter = songHorizontalAdapter
            songListLiveData?.observe(viewLifecycleOwner) { songs ->
                isLoading = false
                songHorizontalAdapter.setItems(songs)
                setSongListPageSubtitle(songs)
            }

            addOnScrollListener(object : PaginationScrollListener(layoutManager as LinearLayoutManager) {
                override fun loadMoreItems() {
                    isLoading = true
                    songListPageViewModel.getSongsByPage(viewLifecycleOwner)
                }

                override fun isLoading(): Boolean = isLoading
            })

            setOnTouchListener { v, _ ->
                hideKeyboard(v)
                false
            }
        }

        bind?.songListSortImageView?.setOnClickListener { view -> showPopupMenu(view, R.menu.sort_song_popup_menu) }
    }

    override fun onCreateOptionsMenu(@NonNull menu: Menu, @NonNull inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.actionView as SearchView?
        searchView?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    songHorizontalAdapter.filter.filter(newText)
                    return false
                }
            })

            setPadding(-32, 0, 0, 0)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showPopupMenu(view: View, menuResource: Int) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(menuResource, menu)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_song_sort_name -> {
                        songHorizontalAdapter.sort(Constants.MEDIA_BY_TITLE)
                        true
                    }
                    R.id.menu_song_sort_most_recently_starred -> {
                        songHorizontalAdapter.sort(Constants.MEDIA_MOST_RECENTLY_STARRED)
                        true
                    }
                    R.id.menu_song_sort_least_recently_starred -> {
                        songHorizontalAdapter.sort(Constants.MEDIA_LEAST_RECENTLY_STARRED)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun setSongListPageSubtitle(children: List<Child>) {
        bind?.pageSubtitleLabel?.text = when (songListPageViewModel.title) {
            Constants.MEDIA_BY_GENRE -> {
                if (children.size < songListPageViewModel.maxNumberByGenre)
                    getString(R.string.generic_list_page_count, children.size)
                else
                    getString(R.string.generic_list_page_count_unknown, songListPageViewModel.maxNumberByGenre)
            }
            Constants.MEDIA_BY_YEAR -> {
                if (children.size < songListPageViewModel.maxNumberByYear)
                    getString(R.string.generic_list_page_count, children.size)
                else
                    getString(R.string.generic_list_page_count_unknown, songListPageViewModel.maxNumberByYear)
            }
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_STARRED -> {
                getString(R.string.generic_list_page_count, children.size)
            }
            else -> null // Or an empty string if it shouldn't be null
        }
    }

    private fun setSongListPageSorter() {
        bind?.songListSortImageView?.visibility = when (songListPageViewModel.title) {
            Constants.MEDIA_BY_GENRE,
            Constants.MEDIA_BY_YEAR -> View.GONE
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_STARRED -> View.VISIBLE
            else -> View.GONE // Default or handle other cases
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
        hideKeyboard(requireView())
        val tracks: List<Child> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelableArrayList(bundle, Constants.TRACKS_OBJECT, Child::class.java)
                ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT) ?: emptyList()
        }
        val position = bundle.getInt(Constants.ITEM_POSITION)
        if (tracks.isNotEmpty()) {
            MediaManager.startQueue(mediaBrowserListenableFuture, tracks, position)
            activity.setBottomSheetInPeek(true)
        }
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }
}
