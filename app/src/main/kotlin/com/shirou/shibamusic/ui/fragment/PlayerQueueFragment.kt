package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.databinding.InnerFragmentPlayerQueueBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.adapter.PlayerSongQueueAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.PlayerBottomSheetViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.Collections
import kotlin.random.Random

@UnstableApi
class PlayerQueueFragment : Fragment(), ClickCallback {

    private var _binding: InnerFragmentPlayerQueueBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerBottomSheetViewModel: PlayerBottomSheetViewModel
    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    private lateinit var playerSongQueueAdapter: PlayerSongQueueAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InnerFragmentPlayerQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playerBottomSheetViewModel = ViewModelProvider(requireActivity())[PlayerBottomSheetViewModel::class.java]
        initQueueRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
        bindMediaController()
    }

    override fun onResume() {
        super.onResume()
        if (::playerSongQueueAdapter.isInitialized) {
            setMediaBrowserListenableFuture()
            updateNowPlayingItem()
        }
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture.addListener({
            try {
                val mediaBrowser = mediaBrowserListenableFuture.get()
                initShuffleButton(mediaBrowser)
                initCleanButton(mediaBrowser)
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaBrowserListenableFuture() {
        // Assuming PlayerSongQueueAdapter has a public property `mediaBrowserListenableFuture`
        playerSongQueueAdapter.mediaBrowserListenableFuture = mediaBrowserListenableFuture
    }

    private fun initQueueRecyclerView() {
        binding.playerQueueRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            playerSongQueueAdapter = PlayerSongQueueAdapter(this@PlayerQueueFragment)
            adapter = playerSongQueueAdapter
        }

        playerBottomSheetViewModel.queueSong.observe(viewLifecycleOwner) { queue ->
            if (queue != null) {
                playerSongQueueAdapter.submitList(queue.map { it as Child })
            }
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            private var originalPosition = -1
            private var fromPosition = -1
            private var toPosition = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1) {
                    originalPosition = viewHolder.bindingAdapterPosition
                }

                fromPosition = viewHolder.bindingAdapterPosition
                toPosition = target.bindingAdapterPosition

                Collections.swap(playerSongQueueAdapter.items, fromPosition, toPosition)
                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                if (originalPosition != -1 && fromPosition != -1 && toPosition != -1) {
                    MediaManager.swap(mediaBrowserListenableFuture, playerSongQueueAdapter.items, originalPosition, toPosition)
                }

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                MediaManager.remove(mediaBrowserListenableFuture, playerSongQueueAdapter.items, viewHolder.bindingAdapterPosition)
                viewHolder.bindingAdapter?.notifyDataSetChanged()
            }
        }).attachToRecyclerView(binding.playerQueueRecyclerView)
    }

    private fun initShuffleButton(mediaBrowser: MediaBrowser) {
        binding.playerShuffleQueueFab.setOnClickListener {
            val startPosition = mediaBrowser.currentMediaItemIndex + 1
            val endPosition = playerSongQueueAdapter.items.size - 1

            if (startPosition < endPosition) {
                val pool = (startPosition..endPosition).toMutableList()

                while (pool.size >= 2) {
                    val fromPositionIndex = Random.nextInt(pool.size)
                    val positionA = pool.removeAt(fromPositionIndex)

                    val toPositionIndex = Random.nextInt(pool.size)
                    val positionB = pool.removeAt(toPositionIndex)

                    Collections.swap(playerSongQueueAdapter.items, positionA, positionB)
                    binding.playerQueueRecyclerView.adapter?.notifyItemMoved(positionA, positionB)
                }

                MediaManager.shuffle(mediaBrowserListenableFuture, playerSongQueueAdapter.items, startPosition, endPosition)
            }
        }
    }

    private fun initCleanButton(mediaBrowser: MediaBrowser) {
        binding.playerCleanQueueButton.setOnClickListener {
            val startPosition = mediaBrowser.currentMediaItemIndex + 1
            val endPosition = playerSongQueueAdapter.items.size

            MediaManager.removeRange(mediaBrowserListenableFuture, playerSongQueueAdapter.items, startPosition, endPosition)
            binding.playerQueueRecyclerView.adapter?.notifyItemRangeRemoved(startPosition, endPosition - startPosition) // endPosition is exclusive
        }
    }

    private fun updateNowPlayingItem() {
        playerSongQueueAdapter.notifyDataSetChanged()
    }

    override fun onMediaClick(bundle: Bundle) {
        val tracks: ArrayList<Child>? = bundle.getParcelableArrayList(Constants.TRACKS_OBJECT)
        val itemPosition = bundle.getInt(Constants.ITEM_POSITION)

        tracks?.let {
            MediaManager.startQueue(mediaBrowserListenableFuture, it, itemPosition)
        }
    }
}
