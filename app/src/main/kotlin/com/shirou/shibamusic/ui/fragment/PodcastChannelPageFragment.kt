package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.OptIn
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentPodcastChannelPageBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.PodcastEpisodeAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.PodcastChannelPageViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PodcastChannelPageFragment : Fragment(), ClickCallback {
    private var _binding: FragmentPodcastChannelPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var podcastChannelPageViewModel: PodcastChannelPageViewModel

    private lateinit var podcastEpisodeAdapter: PodcastEpisodeAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _binding = FragmentPodcastChannelPageBinding.inflate(inflater, container, false)
        podcastChannelPageViewModel = ViewModelProvider(requireActivity())[PodcastChannelPageViewModel::class.java]

        init()
        initAppBar()
        initPodcastChannelInfo()
        initPodcastChannelEpisodesView()

        return binding.root
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

    private fun init() {
        val podcastChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelable(requireArguments(), Constants.PODCAST_CHANNEL_OBJECT, PodcastChannel::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(Constants.PODCAST_CHANNEL_OBJECT)
        }

        podcastChannelPageViewModel.podcastChannel = requireNotNull(podcastChannel) {
            "Podcast channel argument is required"
        }
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.title = podcastChannelPageViewModel.podcastChannel.title
        binding.toolbar.setNavigationOnClickListener { activity.navController.navigateUp() }
        binding.toolbar.title = podcastChannelPageViewModel.podcastChannel.title
    }

    private fun initPodcastChannelInfo() {
        val normalizePodcastChannelDescription = MusicUtil.forceReadableString(podcastChannelPageViewModel.podcastChannel.description)

        binding.apply {
            podcastChannelDescriptionTextView.visibility =
                if (normalizePodcastChannelDescription.trim().isNotEmpty()) View.VISIBLE else View.GONE
            podcastChannelDescriptionTextView.text = normalizePodcastChannelDescription
            podcastEpisodesFilterImageView.setOnClickListener { view ->
                showPopupMenu(view, R.menu.filter_podcast_episode_popup_menu)
            }
        }
    }

    private fun initPodcastChannelEpisodesView() {
        binding.podcastEpisodesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.podcastEpisodesRecyclerView.addItemDecoration(UIUtil.getDividerItemDecoration(requireContext()))

        podcastEpisodeAdapter = PodcastEpisodeAdapter(this)
        binding.podcastEpisodesRecyclerView.adapter = podcastEpisodeAdapter
        podcastChannelPageViewModel.getPodcastChannelEpisodes().observe(viewLifecycleOwner) { channels ->
            val recyclerView = binding.podcastEpisodesRecyclerView
            if (channels.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                podcastEpisodeAdapter.setItems(emptyList())
                return@observe
            }

            val firstChannel = channels.firstOrNull()
            val availableEpisodes = firstChannel?.episodes.orEmpty()

            recyclerView.visibility = if (availableEpisodes.isEmpty()) View.GONE else View.VISIBLE
            podcastEpisodeAdapter.setItems(availableEpisodes)
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

    private fun showPopupMenu(view: View, menuResource: Int) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(menuResource, menu)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_podcast_filter_download -> {
                        podcastEpisodeAdapter.sort(Constants.PODCAST_FILTER_BY_DOWNLOAD)
                        true
                    }
                    R.id.menu_podcast_filter_all -> {
                        podcastEpisodeAdapter.sort(Constants.PODCAST_FILTER_BY_ALL)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onPodcastEpisodeClick(bundle: Bundle) {
        val podcastEpisode = BundleCompat.getParcelable(bundle, Constants.PODCAST_OBJECT, PodcastEpisode::class.java)
        podcastEpisode?.let { MediaManager.startPodcast(mediaBrowserListenableFuture, it) }
        // activity.isBottomSheetInPeek = true
    }

    override fun onPodcastEpisodeLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.podcastEpisodeBottomSheetDialog, bundle)
    }

    override fun onPodcastEpisodeAltClick(bundle: Bundle) {
        val episode = BundleCompat.getParcelable(bundle, Constants.PODCAST_OBJECT, PodcastEpisode::class.java)
            ?: return
        podcastChannelPageViewModel.requestPodcastEpisodeDownload(episode)

        Snackbar.make(requireView(), R.string.podcast_episode_download_request_snackbar, Snackbar.LENGTH_SHORT).apply {
            activity.bind?.bottomNavigation?.let { setAnchorView(it) }
        }.show()
    }
}
