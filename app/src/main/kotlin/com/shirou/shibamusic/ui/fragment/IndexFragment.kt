package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentIndexBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.MusicFolder
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.MusicIndexAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.IndexUtil
import com.shirou.shibamusic.viewmodel.IndexViewModel

@UnstableApi
class IndexFragment : Fragment(), ClickCallback {

    companion object {
        private const val TAG = "IndexFragment"
    }

    private var _binding: FragmentIndexBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var indexViewModel: IndexViewModel
    private lateinit var musicIndexAdapter: MusicIndexAdapter

    private val musicFolder: MusicFolder?
        get() = arguments?.getParcelable(Constants.MUSIC_FOLDER_OBJECT)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentIndexBinding.inflate(inflater, container, false)
        val view = binding.root
        indexViewModel = ViewModelProvider(requireActivity())[IndexViewModel::class.java]

        initAppBar()
        initDirectoryListView()
        init()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun init() {
        indexViewModel.setMusicFolder(musicFolder)

        musicFolder?.let {
            binding.indexTitleLabel.text = it.name
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.let { supportActionBar ->
            supportActionBar.setDisplayHomeAsUpEnabled(true)
            supportActionBar.setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener {
            activity.navController.navigateUp()
        }

        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if ((binding.indexInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.title = indexViewModel.musicFolderName
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    private fun initDirectoryListView() {
        binding.indexRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = MusicIndexAdapter(this@IndexFragment).also { musicIndexAdapter = it }
        }

        indexViewModel.getIndexes(musicFolder?.id).observe(viewLifecycleOwner) { indexes ->
            indexes?.let {
                musicIndexAdapter.setItems(IndexUtil.getArtist(it))
            }
        }

        binding.fastScrollbar.apply {
            setRecyclerView(binding.indexRecyclerView)
            setViewsToUse(R.layout.layout_fast_scrollbar, R.id.fastscroller_bubble, R.id.fastscroller_handle)
        }
    }

    override fun onMusicIndexClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.directoryFragment, bundle)
    }
}
