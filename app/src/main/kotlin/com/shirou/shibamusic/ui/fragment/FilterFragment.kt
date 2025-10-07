package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentFilterBinding
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.FilterViewModel
import com.google.android.material.chip.Chip

@OptIn(UnstableApi::class)
class FilterFragment : Fragment() {
    private companion object {
        const val TAG = "FilterFragment"
    }

    private lateinit var activity: MainActivity
    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!
    private lateinit var filterViewModel: FilterViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        filterViewModel = ViewModelProvider(requireActivity()).get(FilterViewModel::class.java)

        init()
        initAppBar()
        setFilterChips()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        val bundle = Bundle().apply {
            putString(Constants.MEDIA_BY_GENRES, Constants.MEDIA_BY_GENRES)
            putStringArrayList("filters_list", filterViewModel.filters)
            putStringArrayList("filter_name_list", filterViewModel.filterNames)
        }

        binding.finishFilteringTextViewClickable.setOnClickListener {
            if (filterViewModel.filters.size > 1) {
                activity.navController.navigate(R.id.action_filterFragment_to_songListPageFragment, bundle)
            } else {
                Toast.makeText(requireContext(), getString(R.string.filter_info_selection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { activity.navController.navigateUp() }

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            if ((binding.genreFilterInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.filter_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    private fun setFilterChips() {
        filterViewModel.getGenreList().observe(viewLifecycleOwner) { genres ->
            binding.loadingProgressBar.visibility = View.GONE
            binding.filterContainer.visibility = View.VISIBLE
            binding.filtersChipsGroup.removeAllViews()
            for (genre in genres) {
                val genreId = genre.genre ?: continue

                val chip = requireActivity().layoutInflater.inflate(R.layout.chip_search_filter_genre, null, false) as Chip
                chip.text = genreId
                chip.isChecked = filterViewModel.filters.contains(genreId)
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    val label = buttonView.text?.toString() ?: return@setOnCheckedChangeListener
                    if (isChecked) {
                        filterViewModel.addFilter(genreId, label)
                    } else {
                        filterViewModel.removeFilter(genreId, label)
                    }
                }
                binding.filtersChipsGroup.addView(chip)
            }
        }
    }
}
