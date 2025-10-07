package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentDownloadBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.model.DownloadStack
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.DownloadHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.DownloadViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections

@UnstableApi
class DownloadFragment : Fragment(), ClickCallback {

    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private val downloadViewModel: DownloadViewModel by activityViewModels()

    private lateinit var downloadHorizontalAdapter: DownloadHorizontalAdapter

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initDownloadedView()
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
        activity.setBottomNavigationBarVisibility(true)
        activity.setBottomSheetVisibility(true)
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAppBar() {
        val toolbar = binding.root.findViewById<MaterialToolbar>(R.id.toolbar) ?: return

        activity.setSupportActionBar(toolbar)
        toolbar.overflowIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.titleTextColor)
        )
    }

    private fun initDownloadedView() {
        binding.downloadedRecyclerView.setHasFixedSize(true)
        binding.downloadedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        downloadHorizontalAdapter = DownloadHorizontalAdapter(this)
        binding.downloadedRecyclerView.adapter = downloadHorizontalAdapter

        downloadViewModel.getDownloadedTracks(viewLifecycleOwner).observe(viewLifecycleOwner) { songs ->
            songs?.let {
                if (it.isEmpty()) {
                    binding.emptyDownloadLayout.visibility = View.VISIBLE
                    binding.fragmentDownloadNestedScrollView.visibility = View.GONE
                    binding.downloadDownloadedSector.visibility = View.GONE
                    binding.downloadedGroupByImageView.visibility = View.GONE
                } else {
                    binding.emptyDownloadLayout.visibility = View.GONE
                    binding.fragmentDownloadNestedScrollView.visibility = View.VISIBLE
                    binding.downloadDownloadedSector.visibility = View.VISIBLE
                    binding.downloadedGroupByImageView.visibility = View.VISIBLE

                    finishDownloadView(it)
                }
                binding.loadingProgressBar.visibility = View.GONE
            }
        }

        binding.downloadedGroupByImageView.setOnClickListener { view -> showPopupMenu(view, R.menu.download_popup_menu) }
        binding.downloadedGoBackImageView.setOnClickListener { downloadViewModel.popViewStack() }
    }

    private fun finishDownloadView(songs: List<Child>) {
        downloadViewModel.viewStack.observe(viewLifecycleOwner) { stack ->
            if (stack.isEmpty()) {
                return@observe
            }
            binding.downloadedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            val lastLevel = stack.last()

            when (lastLevel.id) {
                Constants.DOWNLOAD_TYPE_TRACK ->
                    downloadHorizontalAdapter.setItems(Constants.DOWNLOAD_TYPE_TRACK, lastLevel.id, lastLevel.view, songs)
                Constants.DOWNLOAD_TYPE_ALBUM ->
                    downloadHorizontalAdapter.setItems(Constants.DOWNLOAD_TYPE_TRACK, lastLevel.id, lastLevel.view, songs)
                Constants.DOWNLOAD_TYPE_ARTIST ->
                    downloadHorizontalAdapter.setItems(Constants.DOWNLOAD_TYPE_ALBUM, lastLevel.id, lastLevel.view, songs)
                Constants.DOWNLOAD_TYPE_GENRE ->
                    downloadHorizontalAdapter.setItems(Constants.DOWNLOAD_TYPE_TRACK, lastLevel.id, lastLevel.view, songs)
                Constants.DOWNLOAD_TYPE_YEAR ->
                    downloadHorizontalAdapter.setItems(Constants.DOWNLOAD_TYPE_TRACK, lastLevel.id, lastLevel.view, songs)
            }

            binding.downloadedGoBackImageView.visibility = if (stack.size > 1) View.VISIBLE else View.GONE

            setupBackPressing(stack.size)
            setupShuffleButton()
        }
    }

    private fun setupShuffleButton() {
        binding.shuffleDownloadedTextViewClickable.setOnClickListener {
            val songs = downloadHorizontalAdapter.getShuffling()

            if (songs != null && songs.isNotEmpty()) {
                Collections.shuffle(songs)

                mediaBrowserListenableFuture?.let { future ->
                    MediaManager.startQueue(future, songs, 0)
                    activity.setBottomSheetInPeek(true)
                }
            }
        }
    }

    private fun setupBackPressing(stackSize: Int) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (stackSize > 1) {
                        downloadViewModel.popViewStack()
                    } else {
                        activity.navController.navigateUp()
                    }
                    remove()
                }
            })
    }

    private fun showPopupMenu(view: View, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(menuResource, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            val newType = when (menuItem.itemId) {
                R.id.menu_download_group_by_track -> Constants.DOWNLOAD_TYPE_TRACK
                R.id.menu_download_group_by_album -> Constants.DOWNLOAD_TYPE_ALBUM
                R.id.menu_download_group_by_artist -> Constants.DOWNLOAD_TYPE_ARTIST
                R.id.menu_download_group_by_genre -> Constants.DOWNLOAD_TYPE_GENRE
                R.id.menu_download_group_by_year -> Constants.DOWNLOAD_TYPE_YEAR
                else -> null
            }
            newType?.let { type ->
                downloadViewModel.initViewStack(DownloadStack(type, null))
                Preferences.setDefaultDownloadViewType(type)
                true
            } ?: false
        }
        popup.show()
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        mediaBrowserListenableFuture?.let {
            MediaBrowser.releaseFuture(it)
        }
        mediaBrowserListenableFuture = null
    }

    override fun onYearClick(bundle: Bundle) {
        downloadViewModel.pushViewStack(
            DownloadStack(Constants.DOWNLOAD_TYPE_YEAR, bundle.getString(Constants.DOWNLOAD_TYPE_YEAR))
        )
    }

    override fun onGenreClick(bundle: Bundle) {
        downloadViewModel.pushViewStack(
            DownloadStack(Constants.DOWNLOAD_TYPE_GENRE, bundle.getString(Constants.DOWNLOAD_TYPE_GENRE))
        )
    }

    override fun onArtistClick(bundle: Bundle) {
        downloadViewModel.pushViewStack(
            DownloadStack(Constants.DOWNLOAD_TYPE_ARTIST, bundle.getString(Constants.DOWNLOAD_TYPE_ARTIST))
        )
    }

    override fun onAlbumClick(bundle: Bundle) {
        downloadViewModel.pushViewStack(
            DownloadStack(Constants.DOWNLOAD_TYPE_ALBUM, bundle.getString(Constants.DOWNLOAD_TYPE_ALBUM))
        )
    }

    override fun onMediaClick(bundle: Bundle) {
        val tracks = bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT)
        val position = bundle.getInt(Constants.ITEM_POSITION)

        mediaBrowserListenableFuture?.let { future ->
            tracks?.let {
                MediaManager.startQueue(future, it, position)
                activity.setBottomSheetInPeek(true)
            }
        }
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onDownloadGroupLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.downloadBottomSheetDialog, bundle)
    }

    companion object {
        private const val TAG = "DownloadFragment"
    }
}
