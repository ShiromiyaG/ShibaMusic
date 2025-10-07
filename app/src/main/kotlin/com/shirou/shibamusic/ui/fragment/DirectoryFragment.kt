package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentDirectoryBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.DialogClickCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.MusicDirectoryAdapter
import com.shirou.shibamusic.ui.dialog.DownloadDirectoryDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.viewmodel.DirectoryViewModel
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class DirectoryFragment : Fragment(), ClickCallback {

    private var _binding: FragmentDirectoryBinding? = null
    private val bind get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var directoryViewModel: DirectoryViewModel

    private lateinit var musicDirectoryAdapter: MusicDirectoryAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    private var menuItem: MenuItem? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(@NonNull menu: Menu, @NonNull inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.directory_page_menu, menu)
        menuItem = menu.getItem(0)
    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        _binding = FragmentDirectoryBinding.inflate(inflater, container, false)
        directoryViewModel = ViewModelProvider(requireActivity()).get(DirectoryViewModel::class.java)

        initAppBar()
        initDirectoryListView()

        return bind.root
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
        _binding = null
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.action_download_directory) {
            val dialog = DownloadDirectoryDialog(object : DialogClickCallback {
                override fun onPositiveClick() {
                    val directoryId = requireArguments().getString(Constants.MUSIC_DIRECTORY_ID)
                        ?: return

                    directoryViewModel.loadMusicDirectory(directoryId).observe(viewLifecycleOwner) { directory ->
                        if (isVisible && activity != null) {
                            val songs = directory.children
                                ?.filter { !it.isDir }
                                ?.toList() ?: emptyList()

                            if (songs.isNotEmpty()) {
                                DownloadUtil.getDownloadTracker(requireContext()).download(
                                    MappingUtil.mapDownloads(songs),
                                    songs.map { Download(it) }
                                )
                            }
                        }
                    }
                }
            })

            dialog.show(activity.supportFragmentManager, null)
            return true
        }

        return false
    }

    private fun initAppBar() {
        activity.setSupportActionBar(bind.toolbar)

        activity.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        bind.toolbar.setNavigationOnClickListener { activity.navController.navigateUp() }
        bind.directoryBackImageView.setOnClickListener { activity.navController.navigateUp() }
    }

    private fun initDirectoryListView() {
        with(bind.directoryRecyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            musicDirectoryAdapter = MusicDirectoryAdapter(this@DirectoryFragment)
            adapter = musicDirectoryAdapter
        }

        val directoryId = requireArguments().getString(Constants.MUSIC_DIRECTORY_ID)
        if (directoryId.isNullOrBlank()) {
            activity.navigateUpIfPossible()
            return
        }

        directoryViewModel.loadMusicDirectory(directoryId).observe(viewLifecycleOwner) { directory ->
            bind.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
                if ((bind.directoryInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(bind.toolbar))) {
                    bind.toolbar.title = directory.name
                } else {
                    bind.toolbar.setTitle(R.string.empty_string)
                }
            }

            bind.directoryTitleLabel.text = directory.name
            musicDirectoryAdapter.setItems(directory.children ?: emptyList())

            menuItem?.isVisible = directory.children?.any { !it.isDir } == true
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
        val tracks = bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT) ?: emptyList()
        MediaManager.startQueue(
            mediaBrowserListenableFuture,
            tracks,
            bundle.getInt(Constants.ITEM_POSITION)
        )
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onMusicDirectoryClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.directoryFragment, bundle)
    }
}
