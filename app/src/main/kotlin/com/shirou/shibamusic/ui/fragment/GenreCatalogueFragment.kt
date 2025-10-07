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
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentGenreCatalogueBinding
import com.shirou.shibamusic.helper.recyclerview.GridItemDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.GenreCatalogueAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.GenreCatalogueViewModel

@OptIn(UnstableApi::class)
class GenreCatalogueFragment : Fragment(), ClickCallback {
    private var _binding: FragmentGenreCatalogueBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var genreCatalogueViewModel: GenreCatalogueViewModel
    private lateinit var genreCatalogueAdapter: GenreCatalogueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentGenreCatalogueBinding.inflate(inflater, container, false)
        genreCatalogueViewModel = ViewModelProvider(requireActivity())[GenreCatalogueViewModel::class.java]

        init()
        initAppBar()
        initGenreCatalogueView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        binding.filterGenresTextViewClickable.setOnClickListener {
            findNavController().navigate(R.id.action_genreCatalogueFragment_to_filterFragment)
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
            findNavController().navigateUp()
        }

        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if ((binding.genreInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.genre_catalogue_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGenreCatalogueView() {
        binding.genreCatalogueRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(GridItemDecoration(2, 16, false))
            setHasFixedSize(true)
        }

        genreCatalogueAdapter = GenreCatalogueAdapter(this)
        genreCatalogueAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.genreCatalogueRecyclerView.adapter = genreCatalogueAdapter

        genreCatalogueViewModel.getGenreList().observe(viewLifecycleOwner) { genres ->
            genreCatalogueAdapter.setItems(genres)
        }

        binding.genreCatalogueRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false // Returning false means the event is not consumed
        }

        binding.genreListSortImageView.setOnClickListener { view ->
            showPopupMenu(view, R.menu.sort_genre_popup_menu)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    genreCatalogueAdapter.filter.filter(newText)
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
                    R.id.menu_genre_sort_name -> {
                        genreCatalogueAdapter.sort(Constants.GENRE_ORDER_BY_NAME)
                        true
                    }
                    R.id.menu_genre_sort_random -> {
                        genreCatalogueAdapter.sort(Constants.GENRE_ORDER_BY_RANDOM)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onGenreClick(bundle: Bundle) {
        findNavController().navigate(R.id.songListPageFragment, bundle)
        hideKeyboard(requireView())
    }
}
