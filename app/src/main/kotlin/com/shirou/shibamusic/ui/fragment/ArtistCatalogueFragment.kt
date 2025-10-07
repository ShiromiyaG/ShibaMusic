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
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentArtistCatalogueBinding
import com.shirou.shibamusic.helper.recyclerview.GridItemDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.ArtistCatalogueAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.ArtistCatalogueViewModel

@UnstableApi
class ArtistCatalogueFragment : Fragment(), ClickCallback {

    companion object {
        private const val TAG = "ArtistCatalogueFragment"
    }

    private var _binding: FragmentArtistCatalogueBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var artistCatalogueViewModel: ArtistCatalogueViewModel
    private lateinit var artistAdapter: ArtistCatalogueAdapter

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentArtistCatalogueBinding.inflate(inflater, container, false)
        val view = binding.root
        initAppBar()
        initArtistCatalogueView()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initData() {
        artistCatalogueViewModel = ViewModelProvider(requireActivity())[ArtistCatalogueViewModel::class.java]
        artistCatalogueViewModel.loadArtists()
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
                binding.toolbar.setTitle(R.string.artist_catalogue_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initArtistCatalogueView() {
        binding.artistCatalogueRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(GridItemDecoration(2, 20, false))
            setHasFixedSize(true)
            artistAdapter = ArtistCatalogueAdapter(this@ArtistCatalogueFragment)
            artistAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = artistAdapter
            setOnTouchListener { v, _ ->
                hideKeyboard(v)
                false
            }
        }

        artistCatalogueViewModel.artistList.observe(viewLifecycleOwner) { artistList ->
            artistAdapter.setItems(artistList)
        }

        binding.artistListSortImageView.setOnClickListener { view ->
            showPopupMenu(view, R.menu.sort_artist_popup_menu)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = getString(R.string.filter_artist)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // this toast may be overkill...
                Toast.makeText(requireContext(), "Search: $query", Toast.LENGTH_SHORT).show()
                filterArtists(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterArtists(newText)
                return true
            }
        })

        searchView.setPadding(-32, 0, 0, 0)
    }

    private fun filterArtists(query: String?) {
        val allArtists = artistCatalogueViewModel.artistList.value

        if (allArtists.isNullOrEmpty()) {
            return
        }

        if (query.isNullOrBlank()) {
            artistAdapter.setItems(allArtists)
        } else {
            val searchQuery = query.lowercase().trim()
            val filteredArtists = allArtists.filter { artist ->
                artist.name?.lowercase()?.contains(searchQuery) == true
            }
            artistAdapter.setItems(filteredArtists)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showPopupMenu(view: View, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(menuResource, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_artist_sort_name -> {
                    artistAdapter.sort(Constants.ARTIST_ORDER_BY_NAME)
                    true
                }
                R.id.menu_artist_sort_random -> {
                    artistAdapter.sort(Constants.ARTIST_ORDER_BY_RANDOM)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onArtistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onArtistLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }
}
