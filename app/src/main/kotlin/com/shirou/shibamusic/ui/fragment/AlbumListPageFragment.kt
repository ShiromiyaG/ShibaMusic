package com.shirou.shibamusic.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.widget.SearchView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentAlbumListPageBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.AlbumListPageViewModel
import kotlin.OptIn

@OptIn(UnstableApi::class)
class AlbumListPageFragment : Fragment(), ClickCallback {
    private var _binding: FragmentAlbumListPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var albumListPageViewModel: AlbumListPageViewModel
    private lateinit var albumHorizontalAdapter: AlbumHorizontalAdapter

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentAlbumListPageBinding.inflate(inflater, container, false)
        val view = binding.root
        albumListPageViewModel = ViewModelProvider(requireActivity())[AlbumListPageViewModel::class.java]

        init()
        initAppBar()
        initAlbumListView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        val args = requireArguments()
        when {
            args.getString(Constants.ALBUM_RECENTLY_PLAYED) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_RECENTLY_PLAYED
                binding.pageTitleLabel.setText(R.string.album_list_page_recently_played)
            }
            args.getString(Constants.ALBUM_MOST_PLAYED) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_MOST_PLAYED
                binding.pageTitleLabel.setText(R.string.album_list_page_most_played)
            }
            args.getString(Constants.ALBUM_RECENTLY_ADDED) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_RECENTLY_ADDED
                binding.pageTitleLabel.setText(R.string.album_list_page_recently_added)
            }
            args.getString(Constants.ALBUM_STARRED) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_STARRED
                binding.pageTitleLabel.setText(R.string.album_list_page_starred)
            }
            args.getString(Constants.ALBUM_NEW_RELEASES) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_NEW_RELEASES
                binding.pageTitleLabel.setText(R.string.album_list_page_new_releases)
            }
            args.getString(Constants.ALBUM_DOWNLOADED) != null -> {
                albumListPageViewModel.title = Constants.ALBUM_DOWNLOADED
                binding.pageTitleLabel.setText(R.string.album_list_page_downloaded)
            }
            else -> {
                val artistObject: ArtistID3? = args.getParcelable(Constants.ARTIST_OBJECT)
                if (artistObject != null) {
                    albumListPageViewModel.artist = artistObject
                    albumListPageViewModel.title = Constants.ALBUM_FROM_ARTIST
                    binding.pageTitleLabel.text = albumListPageViewModel.artist?.name.orEmpty()
                }
            }
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { v ->
            hideKeyboard(v)
            activity.navigateUpIfPossible()
        }

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            if ((binding.albumInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.album_list_page_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initAlbumListView() {
        binding.albumListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        albumHorizontalAdapter = AlbumHorizontalAdapter(
            this,
            (albumListPageViewModel.title == Constants.ALBUM_DOWNLOADED || albumListPageViewModel.title == Constants.ALBUM_FROM_ARTIST)
        )

        binding.albumListRecyclerView.adapter = albumHorizontalAdapter
        albumListPageViewModel.getAlbumList(viewLifecycleOwner).observe(viewLifecycleOwner) { albums ->
            albumHorizontalAdapter.setItems(albums)
            setAlbumListPageSubtitle(albums)
            setAlbumListPageSorter()
        }

        binding.albumListRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false
        }

        binding.albumListSortImageView.setOnClickListener { view ->
            showPopupMenu(view, R.menu.sort_horizontal_album_popup_menu)
        }
    }

    override fun onCreateOptionsMenu(@NonNull menu: Menu, @NonNull inflater: MenuInflater) {
        inflater.inflate(R.menu.artist_list_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.actionView as SearchView
        searchView.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    albumHorizontalAdapter.filter.filter(newText)
                    return false
                }
            })
            setPadding(-32, 0, 0, 0)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showPopupMenu(view: View, menuResource: Int) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(menuResource, menu)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_horizontal_album_sort_name -> {
                        albumHorizontalAdapter.sort(Constants.ALBUM_ORDER_BY_NAME)
                        true
                    }
                    R.id.menu_horizontal_album_sort_most_recently_starred -> {
                        albumHorizontalAdapter.sort(Constants.ALBUM_ORDER_BY_MOST_RECENTLY_STARRED)
                        true
                    }
                    R.id.menu_horizontal_album_sort_least_recently_starred -> {
                        albumHorizontalAdapter.sort(Constants.ALBUM_ORDER_BY_LEAST_RECENTLY_STARRED)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun setAlbumListPageSubtitle(albums: List<AlbumID3>) {
        when (albumListPageViewModel.title) {
            Constants.ALBUM_RECENTLY_PLAYED,
            Constants.ALBUM_MOST_PLAYED,
            Constants.ALBUM_RECENTLY_ADDED -> {
                binding.pageSubtitleLabel.text = if (albums.size < albumListPageViewModel.maxNumber) {
                    getString(R.string.generic_list_page_count, albums.size)
                } else {
                    getString(R.string.generic_list_page_count_unknown, albumListPageViewModel.maxNumber)
                }
            }
            Constants.ALBUM_STARRED -> {
                binding.pageSubtitleLabel.text = getString(R.string.generic_list_page_count, albums.size)
            }
        }
    }

    private fun setAlbumListPageSorter() {
        when (albumListPageViewModel.title) {
            Constants.ALBUM_RECENTLY_PLAYED,
            Constants.ALBUM_MOST_PLAYED,
            Constants.ALBUM_RECENTLY_ADDED -> {
                binding.albumListSortImageView.visibility = View.GONE
            }
            Constants.ALBUM_STARRED -> {
                binding.albumListSortImageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onAlbumClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }
}
