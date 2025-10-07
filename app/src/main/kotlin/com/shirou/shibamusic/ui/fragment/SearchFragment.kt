package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import com.shirou.shibamusic.databinding.FragmentSearchBinding
import com.shirou.shibamusic.helper.recyclerview.CustomLinearSnapHelper
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.AlbumAdapter
import com.shirou.shibamusic.ui.adapter.ArtistAdapter
import com.shirou.shibamusic.ui.adapter.SongHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.SearchViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.shirou.shibamusic.subsonic.models.Child

@OptIn(UnstableApi::class)
class SearchFragment : Fragment(), ClickCallback {

    private var _bind: FragmentSearchBinding? = null
    private val bind get() = _bind!!

    private lateinit var activity: MainActivity
    private lateinit var searchViewModel: SearchViewModel

    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var songHorizontalAdapter: SongHorizontalAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity

        _bind = FragmentSearchBinding.inflate(inflater, container, false)
        searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]

        initSearchResultView()
        initSearchView()
        inputFocus()

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
        _bind = null
    }

    private fun initSearchResultView() {
        bind.searchResultArtistRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            artistAdapter = ArtistAdapter(this@SearchFragment, false, false)
            adapter = artistAdapter
            CustomLinearSnapHelper().attachToRecyclerView(this)
        }

        bind.searchResultAlbumRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            albumAdapter = AlbumAdapter(this@SearchFragment)
            adapter = albumAdapter
            CustomLinearSnapHelper().attachToRecyclerView(this)
        }

        bind.searchResultTracksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            songHorizontalAdapter = SongHorizontalAdapter(this@SearchFragment, true, false, null)
            adapter = songHorizontalAdapter
        }
    }

    private fun initSearchView() {
        setRecentSuggestions()

        bind.searchView.editText?.setOnEditorActionListener { _, _, _ ->
            val query = bind.searchView.text.toString()
            if (isQueryValid(query)) {
                search(query)
                true
            } else {
                false
            }
        }

        bind.searchView.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                if (start + count > 1) {
                    setSearchSuggestions(charSequence.toString())
                } else {
                    setRecentSuggestions()
                }
            }

            override fun afterTextChanged(editable: Editable?) {
                // Not used
            }
        })
    }

    fun setRecentSuggestions() {
        bind.searchViewSuggestionContainer.apply {
            removeAllViews()
            for (suggestion in searchViewModel.getRecentSearchSuggestion()) {
                LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, this, false).also { view ->
                    val leadingImageView = view.findViewById<ImageView>(R.id.search_suggestion_icon)
                    val titleView = view.findViewById<TextView>(R.id.search_suggestion_title)
                    val tailingImageView = view.findViewById<ImageView>(R.id.search_suggestion_delete_icon)

                    leadingImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_history, null))
                    titleView.text = suggestion

                    view.setOnClickListener { search(suggestion) }

                    tailingImageView.setOnClickListener {
                        searchViewModel.deleteRecentSearch(suggestion)
                        setRecentSuggestions()
                    }
                    addView(view)
                }
            }
        }
    }

    fun setSearchSuggestions(query: String) {
        searchViewModel.getSearchSuggestion(query).observe(viewLifecycleOwner) { suggestions ->
            bind.searchViewSuggestionContainer.apply {
                removeAllViews()
                for (suggestion in suggestions) {
                    LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, this, false).also { view ->
                        val leadingImageView = view.findViewById<ImageView>(R.id.search_suggestion_icon)
                        val titleView = view.findViewById<TextView>(R.id.search_suggestion_title)
                        val tailingImageView = view.findViewById<ImageView>(R.id.search_suggestion_delete_icon)

                        leadingImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_search, null))
                        titleView.text = suggestion
                        tailingImageView.visibility = View.GONE

                        view.setOnClickListener { search(suggestion) }
                        addView(view)
                    }
                }
            }
        }
    }

    fun search(query: String) {
        searchViewModel.query = query
        bind.searchBar.setText(query)
        bind.searchView.hide()
        performSearch(query)
    }

    private fun performSearch(query: String) {
        searchViewModel.search3(query).observe(viewLifecycleOwner) { result ->
            _bind?.let { binding ->
                result.artists?.let { artists ->
                    binding.searchArtistSector.visibility = if (artists.isNotEmpty()) View.VISIBLE else View.GONE
                    artistAdapter.artists = artists
                } ?: run {
                    artistAdapter.artists = emptyList()
                    binding.searchArtistSector.visibility = View.GONE
                }

                result.albums?.let { albums ->
                    binding.searchAlbumSector.visibility = if (albums.isNotEmpty()) View.VISIBLE else View.GONE
                    albumAdapter.setItems(albums)
                } ?: run {
                    albumAdapter.setItems(emptyList())
                    binding.searchAlbumSector.visibility = View.GONE
                }

                result.songs?.let { songs ->
                    binding.searchSongSector.visibility = if (songs.isNotEmpty()) View.VISIBLE else View.GONE
                    songHorizontalAdapter.setItems(songs)
                } ?: run {
                    songHorizontalAdapter.setItems(emptyList())
                    binding.searchSongSector.visibility = View.GONE
                }
            }
        }
        _bind?.searchResultLayout?.visibility = View.VISIBLE
    }

    private fun isQueryValid(query: String): Boolean {
        return query.isNotBlank() && query.trim().length > 2
    }

    private fun inputFocus() {
        bind.searchView.show()
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
        val tracks: List<Child> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BundleCompat.getParcelableArrayList(bundle, Constants.TRACKS_OBJECT, Child::class.java)
                ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT) ?: emptyList()
        }
        val position = bundle.getInt(Constants.ITEM_POSITION)
        if (tracks.isNotEmpty()) {
            MediaManager.startQueue(mediaBrowserListenableFuture, tracks, position)
            activity.setBottomSheetInPeek(true)
        }
    }

    override fun onMediaLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onAlbumClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    override fun onArtistClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }

    companion object {
        private const val TAG = "SearchFragment"
    }
}
