package com.shirou.shibamusic.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.R
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.viewmodel.ArtistBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class ArtistBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var artistBottomSheetViewModel: ArtistBottomSheetViewModel
    private lateinit var artist: ArtistID3

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_artist_dialog, container, false)

        artist = requireArguments().getParcelable(Constants.ARTIST_OBJECT)!! // Assuming ArtistID3 is always present

        artistBottomSheetViewModel = ViewModelProvider(requireActivity())[ArtistBottomSheetViewModel::class.java]
        artistBottomSheetViewModel.artist = artist // Using property syntax

        init(view)

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    // TODO Utilizzare il viewmodel come tramite ed evitare le chiamate dirette
    private fun init(view: View) {
        with(view) {
            val coverArtist = findViewById<ImageView>(R.id.artist_cover_image_view)
            CustomGlideRequest.Builder
                .from(requireContext(), artistBottomSheetViewModel.artist.coverArtId, CustomGlideRequest.ResourceType.Artist)
                .build()
                .into(coverArtist)

            val nameArtist = findViewById<TextView>(R.id.song_title_text_view)
            nameArtist.apply {
                text = artistBottomSheetViewModel.artist.name
                isSelected = true
            }

            val favoriteToggle = findViewById<ToggleButton>(R.id.button_favorite)
            favoriteToggle.apply {
                isChecked = artistBottomSheetViewModel.artist.starred != null
                setOnClickListener { artistBottomSheetViewModel.setFavorite() }
            }

            val playRadio = findViewById<TextView>(R.id.play_radio_text_view)
            playRadio.setOnClickListener {
                val artistRepository = ArtistRepository()

                artistRepository.getInstantMix(artist, 20).observe(viewLifecycleOwner) { songs ->
                    MusicUtil.ratingFilter(songs)

                    if (songs.isNotEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                    }

                    dismissBottomSheet()
                }
            }

            val playRandom = findViewById<TextView>(R.id.play_random_text_view)
            playRandom.setOnClickListener {
                val artistRepository = ArtistRepository()
                artistRepository.getRandomSong(artist, 50).observe(viewLifecycleOwner) { songs ->
                    MusicUtil.ratingFilter(songs)

                    if (songs.isNotEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                        dismissBottomSheet()
                    } else {
                        Toast.makeText(requireContext(), R.string.artist_error_retrieving_tracks, Toast.LENGTH_SHORT).show()
                    }
                    dismissBottomSheet()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
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
}
