package com.shirou.shibamusic.ui.fragment

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
import android.widget.SearchView

import android.annotation.SuppressLint
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentArtistListPageBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.ArtistHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.ArtistListPageViewModel

@UnstableApi
class ArtistListPageFragment : Fragment(), ClickCallback {
    private var _binding: FragmentArtistListPageBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var artistListPageViewModel: ArtistListPageViewModel

    private lateinit var artistHorizontalAdapter: ArtistHorizontalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentArtistListPageBinding.inflate(inflater, container, false)
        artistListPageViewModel = ViewModelProvider(requireActivity())[ArtistListPageViewModel::class.java]

        init()
        initAppBar()
        initArtistListView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        if (requireArguments().getString(Constants.ARTIST_STARRED) != null) {
            artistListPageViewModel.title = Constants.ARTIST_STARRED
            binding.pageTitleLabel.setText(R.string.artist_list_page_starred)
        } else if (requireArguments().getString(Constants.ARTIST_DOWNLOADED) != null) {
            artistListPageViewModel.title = Constants.ARTIST_DOWNLOADED
            binding.pageTitleLabel.setText(R.string.artist_list_page_downloaded)
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { v ->
            hideKeyboard(v)
            activity.navController.navigateUp()
        }

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            if ((binding.artistInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.artist_list_page_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initArtistListView() {
        binding.artistListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.artistListRecyclerView.setHasFixedSize(true)

        artistHorizontalAdapter = ArtistHorizontalAdapter(this)
        binding.artistListRecyclerView.adapter = artistHorizontalAdapter
        artistListPageViewModel.getArtistList(viewLifecycleOwner).observe(viewLifecycleOwner) { artists ->
            artistHorizontalAdapter.setItems(artists)
            setArtistListPageSubtitle(artists)
            setArtistListPageSorter()
        }

        binding.artistListRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false
        }

        binding.artistListSortImageView.setOnClickListener { view -> showPopupMenu(view, R.menu.sort_horizontal_artist_popup_menu) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        // Original Java code used android.widget.SearchView
        (searchItem.actionView as? SearchView)?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // newText is nullable from SearchView.OnQueryTextListener contract
                    // .orEmpty() handles null by converting it to an empty string.
                    artistHorizontalAdapter.filter.filter(newText.orEmpty())
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
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(menuResource, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_horizontal_artist_sort_name -> {
                    artistHorizontalAdapter.sort(Constants.ARTIST_ORDER_BY_NAME)
                    true
                }
                R.id.menu_horizontal_artist_sort_most_recently_starred -> {
                    artistHorizontalAdapter.sort(Constants.ARTIST_ORDER_BY_MOST_RECENTLY_STARRED)
                    true
                }
                R.id.menu_horizontal_artist_sort_least_recently_starred -> {
                    artistHorizontalAdapter.sort(Constants.ARTIST_ORDER_BY_LEAST_RECENTLY_STARRED)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun setArtistListPageSubtitle(artists: List<ArtistID3>) {
        when (artistListPageViewModel.title) {
            Constants.ARTIST_STARRED, Constants.ARTIST_DOWNLOADED -> {
                binding.pageSubtitleLabel.setText(getString(R.string.generic_list_page_count, artists.size))
            }
        }
    }

    private fun setArtistListPageSorter() {
        when (artistListPageViewModel.title) {
            Constants.ARTIST_STARRED, Constants.ARTIST_DOWNLOADED -> {
                binding.artistListSortImageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onArtistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }
}
