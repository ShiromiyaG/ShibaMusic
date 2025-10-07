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
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentPlaylistCatalogueBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.PlaylistHorizontalAdapter
import com.shirou.shibamusic.ui.dialog.PlaylistEditorDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.PlaylistCatalogueViewModel

@UnstableApi
class PlaylistCatalogueFragment : Fragment(), ClickCallback {
    private var _binding: FragmentPlaylistCatalogueBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var playlistCatalogueViewModel: PlaylistCatalogueViewModel
    private lateinit var playlistHorizontalAdapter: PlaylistHorizontalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentPlaylistCatalogueBinding.inflate(inflater, container, false)
        playlistCatalogueViewModel = ViewModelProvider(requireActivity())[PlaylistCatalogueViewModel::class.java]

        init()
        initAppBar()
        initPlaylistCatalogueView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        val playlistAll = requireArguments().getString(Constants.PLAYLIST_ALL)
        val playlistDownloaded = requireArguments().getString(Constants.PLAYLIST_DOWNLOADED)

        when {
            playlistAll != null -> playlistCatalogueViewModel.type = Constants.PLAYLIST_ALL
            playlistDownloaded != null -> playlistCatalogueViewModel.type = Constants.PLAYLIST_DOWNLOADED
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
            activity.navController.navigateUp()
        }

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            if ((binding.albumInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.playlist_catalogue_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPlaylistCatalogueView() {
        binding.playlistCatalogueRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        playlistHorizontalAdapter = PlaylistHorizontalAdapter(this)
        binding.playlistCatalogueRecyclerView.adapter = playlistHorizontalAdapter

        playlistCatalogueViewModel.getPlaylistList(viewLifecycleOwner).observe(viewLifecycleOwner) { playlists ->
            playlists?.let { playlistHorizontalAdapter.setItems(it) }
        }

        binding.playlistCatalogueRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false
        }

        binding.playlistListSortImageView.setOnClickListener { view -> showPopupMenu(view, R.menu.sort_playlist_popup_menu) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                playlistHorizontalAdapter.filter.filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)
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
                R.id.menu_playlist_sort_name -> {
                    playlistHorizontalAdapter.sort(Constants.GENRE_ORDER_BY_NAME)
                    true
                }
                R.id.menu_playlist_sort_random -> {
                    playlistHorizontalAdapter.sort(Constants.GENRE_ORDER_BY_RANDOM)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun onPlaylistClick(bundle: Bundle) {
        bundle.putBoolean("is_offline", false)
        Navigation.findNavController(requireView()).navigate(R.id.playlistPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onPlaylistLongClick(bundle: Bundle) {
        val dialog = PlaylistEditorDialog(null)
        dialog.arguments = bundle
        dialog.show(activity.supportFragmentManager, null)
        hideKeyboard(requireView())
    }
}
