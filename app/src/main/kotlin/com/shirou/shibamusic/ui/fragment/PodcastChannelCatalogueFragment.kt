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
import android.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentPodcastChannelCatalogueBinding
import com.shirou.shibamusic.helper.recyclerview.GridItemDecoration
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.PodcastChannelCatalogueAdapter
import com.shirou.shibamusic.viewmodel.PodcastChannelCatalogueViewModel
import kotlin.OptIn

@OptIn(UnstableApi::class)
class PodcastChannelCatalogueFragment : Fragment(), ClickCallback {

    private var _binding: FragmentPodcastChannelCatalogueBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var podcastChannelCatalogueViewModel: PodcastChannelCatalogueViewModel
    private lateinit var podcastChannelCatalogueAdapter: PodcastChannelCatalogueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentPodcastChannelCatalogueBinding.inflate(inflater, container, false)
        podcastChannelCatalogueViewModel = ViewModelProvider(requireActivity())[PodcastChannelCatalogueViewModel::class.java]

        initAppBar()
        initPodcastChannelCatalogueView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            if ((binding.podcastChannelInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.podcast_channel_catalogue_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initPodcastChannelCatalogueView() {
        binding.podcastChannelCatalogueRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.podcastChannelCatalogueRecyclerView.addItemDecoration(GridItemDecoration(2, 20, false))
        binding.podcastChannelCatalogueRecyclerView.setHasFixedSize(true)

        podcastChannelCatalogueAdapter = PodcastChannelCatalogueAdapter(this)
        podcastChannelCatalogueAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.podcastChannelCatalogueRecyclerView.adapter = podcastChannelCatalogueAdapter
        podcastChannelCatalogueViewModel.getPodcastChannels(viewLifecycleOwner).observe(viewLifecycleOwner) { albums ->
            albums?.let {
                podcastChannelCatalogueAdapter.setItems(it)
            }
        }

        binding.podcastChannelCatalogueRecyclerView.setOnTouchListener { v, _ ->
            hideKeyboard(v)
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                podcastChannelCatalogueAdapter.filter.filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)
    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onPodcastChannelClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.podcastChannelPageFragment, bundle)
        hideKeyboard(requireView())
    }

    override fun onPodcastChannelLongClick(bundle: Bundle) {
        // Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    companion object {
        private const val TAG = "PodcastChannelCatalogue"
    }
}
