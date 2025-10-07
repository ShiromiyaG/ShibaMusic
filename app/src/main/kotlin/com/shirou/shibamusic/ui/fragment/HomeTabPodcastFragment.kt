package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager

import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentHomeTabPodcastBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.PodcastCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.PodcastChannelHorizontalAdapter
import com.shirou.shibamusic.ui.adapter.PodcastEpisodeAdapter
import com.shirou.shibamusic.ui.dialog.PodcastChannelEditorDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.util.UIUtil
import com.shirou.shibamusic.viewmodel.PodcastViewModel
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class HomeTabPodcastFragment : Fragment(), ClickCallback, PodcastCallback {
    private val TAG = "HomeTabPodcastFragment"

    private var bind: FragmentHomeTabPodcastBinding? = null
    private var activity: MainActivity? = null
    private lateinit var podcastViewModel: PodcastViewModel

    private lateinit var podcastEpisodeAdapter: PodcastEpisodeAdapter
    private lateinit var podcastChannelHorizontalAdapter: PodcastChannelHorizontalAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = getActivity() as? MainActivity

        bind = FragmentHomeTabPodcastBinding.inflate(inflater, container, false)
        val view = bind!!.root
        podcastViewModel = ViewModelProvider(requireActivity())[PodcastViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        initPodcastView()
        initNewestPodcastsView()
        initPodcastChannelsView()
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
        bind = null
    }

    private fun init() {
        bind?.podcastChannelsPreTextView?.setOnClickListener {
            val dialog = PodcastChannelEditorDialog(this@HomeTabPodcastFragment)
            activity?.supportFragmentManager?.let { fm -> dialog.show(fm, null) }
        }

        bind?.podcastChannelsTextViewClickable?.setOnClickListener {
            activity?.navigateIfPossible(R.id.action_homeFragment_to_podcastChannelCatalogueFragment)
        }
        bind?.hideSectionButton?.setOnClickListener { Preferences.setPodcastSectionHidden() }
    }

    private fun initPodcastView() {
        podcastViewModel.getPodcastChannels(viewLifecycleOwner).observe(viewLifecycleOwner) { podcastChannels ->
            bind?.apply {
                val hasChannels = podcastChannels.isNotEmpty()
                homePodcastChannelsSector.visibility = if (hasChannels) View.VISIBLE else View.GONE
                emptyPodcastLayout.visibility = if (hasChannels) View.GONE else View.VISIBLE
            }
        }
    }

    private fun initPodcastChannelsView() {
        bind?.podcastChannelsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        podcastChannelHorizontalAdapter = PodcastChannelHorizontalAdapter(this)
        bind?.podcastChannelsRecyclerView?.adapter = podcastChannelHorizontalAdapter
        podcastViewModel.getPodcastChannels(viewLifecycleOwner).observe(viewLifecycleOwner) { podcastChannels ->
            bind?.apply {
                homePodcastChannelsSector.visibility = if (podcastChannels.isEmpty()) View.GONE else View.VISIBLE
                podcastChannelHorizontalAdapter.setItems(podcastChannels)
            }
        }
    }

    private fun initNewestPodcastsView() {
        bind?.newestPodcastsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        bind?.newestPodcastsRecyclerView?.addItemDecoration(UIUtil.getDividerItemDecoration(requireContext()))

        podcastEpisodeAdapter = PodcastEpisodeAdapter(this)
        bind?.newestPodcastsRecyclerView?.adapter = podcastEpisodeAdapter
        podcastViewModel.getNewestPodcastEpisodes(viewLifecycleOwner).observe(viewLifecycleOwner) { podcastEpisodes ->
            bind?.apply {
                homeNewestPodcastsSector.visibility = if (podcastEpisodes.isEmpty()) View.GONE else View.VISIBLE
                podcastEpisodeAdapter.setItems(podcastEpisodes)
            }
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

    override fun onPodcastEpisodeClick(bundle: Bundle) {
        val podcastEpisode = BundleCompat.getParcelable(bundle, Constants.PODCAST_OBJECT, PodcastEpisode::class.java)
            ?: return
        MediaManager.startPodcast(mediaBrowserListenableFuture, podcastEpisode)
        activity?.setBottomSheetInPeek(true)
    }

    override fun onPodcastEpisodeLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.podcastEpisodeBottomSheetDialog, bundle)
    }

    override fun onPodcastChannelClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.podcastChannelPageFragment, bundle)
    }

    override fun onPodcastChannelLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.podcastChannelBottomSheetDialog, bundle)
    }

    override fun onDismiss() {
        Handler().postDelayed({
            podcastViewModel.refreshPodcastChannels(viewLifecycleOwner)
            podcastViewModel.refreshNewestPodcastEpisodes(viewLifecycleOwner)
        }, 1000)
    }
}
