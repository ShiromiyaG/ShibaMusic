package com.shirou.shibamusic.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentAlbumCatalogueBinding
import com.shirou.shibamusic.helper.recyclerview.GridItemDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumCatalogueAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.AlbumCatalogueViewModel

@OptIn(UnstableApi::class)
class AlbumCatalogueFragment : Fragment(), ClickCallback {

    private var _binding: FragmentAlbumCatalogueBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var albumCatalogueViewModel: AlbumCatalogueViewModel
    private lateinit var albumAdapter: AlbumCatalogueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        albumCatalogueViewModel.stopLoading()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentAlbumCatalogueBinding.inflate(inflater, container, false)
        val view = binding.root

        initAppBar()
        initAlbumCatalogueView()
        initProgressLoader()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initData() {
        albumCatalogueViewModel = ViewModelProvider(requireActivity())[AlbumCatalogueViewModel::class.java]
        albumCatalogueViewModel.loadAlbums()
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { v ->
            hideKeyboard(v)
            activity.navController.navigateUp()
        }

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            if ((binding.albumInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.album_catalogue_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initAlbumCatalogueView() {
        binding.albumCatalogueRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(GridItemDecoration(2, 20, false))
            setHasFixedSize(true)
        }

        albumAdapter = AlbumCatalogueAdapter(this, true).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.albumCatalogueRecyclerView.adapter = albumAdapter
        albumCatalogueViewModel.albumList.observe(viewLifecycleOwner) { albums ->
            albumAdapter.setItems(albums)
        }

        binding.albumCatalogueRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false
        }

        binding.albumListSortImageView.setOnClickListener { view ->
            showPopupMenu(view, R.menu.sort_album_popup_menu)
        }
    }

    private fun initProgressLoader() {
        albumCatalogueViewModel.loadingStatus.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.albumListSortImageView.isEnabled = false
                binding.albumListProgressLoader.visibility = View.VISIBLE
            } else {
                binding.albumListSortImageView.isEnabled = true
                binding.albumListProgressLoader.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem: MenuItem? = menu.findItem(R.id.action_search)

        (searchItem?.actionView as? SearchView)?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    albumAdapter.filter.filter(newText)
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
                    R.id.menu_album_sort_name -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_NAME)
                    R.id.menu_album_sort_artist -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_ARTIST)
                    R.id.menu_album_sort_year -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_YEAR)
                    R.id.menu_album_sort_random -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_RANDOM)
                    R.id.menu_album_sort_recently_added -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_RECENTLY_ADDED)
                    R.id.menu_album_sort_recently_played -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_RECENTLY_PLAYED)
                    R.id.menu_album_sort_most_played -> albumAdapter.sort(Constants.ALBUM_ORDER_BY_MOST_PLAYED)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    override fun onAlbumClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onAlbumLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    companion object {
        private const val TAG = "ArtistCatalogueFragment"
    }
}
