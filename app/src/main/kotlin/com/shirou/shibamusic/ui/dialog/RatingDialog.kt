package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogRatingBinding
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.RatingViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RatingDialog : DialogFragment() {
    private companion object {
        private const val TAG = "ServerSignupDialog"
    }

    private var bind: DialogRatingBinding? = null
    private lateinit var ratingViewModel: RatingViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogRatingBinding.inflate(layoutInflater)
        ratingViewModel = ViewModelProvider(requireActivity())[RatingViewModel::class.java]

        return MaterialAlertDialogBuilder(requireActivity()).apply {
            // 'bind' is guaranteed non-null after its initialization within this method
            setView(bind!!.root)
            setTitle(R.string.rating_dialog_title)
            setNegativeButton(R.string.rating_dialog_negative_button) { dialog, _ -> dialog.cancel() }
            setPositiveButton(R.string.rating_dialog_positive_button) { _, _ ->
                // 'bind' is guaranteed non-null while the dialog is active and buttons are clickable
                bind!!.ratingBar.rating.let { rating ->
                    ratingViewModel.rate(rating.toInt())
                }
            }
        }.create()
    }

    override fun onStart() {
        super.onStart()
        setElementInfo()
        setRating()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setElementInfo() {
        val args = requireArguments()

        args.getParcelable<Child>(Constants.TRACK_OBJECT)?.let { track ->
            ratingViewModel.song = track
        } ?: args.getParcelable<AlbumID3>(Constants.ALBUM_OBJECT)?.let { album ->
            ratingViewModel.album = album
        } ?: args.getParcelable<ArtistID3>(Constants.ARTIST_OBJECT)?.let { artist ->
            ratingViewModel.artist = artist
        }
    }

    private fun setRating() {
        val lifecycleOwner = viewLifecycleOwner

        // Assuming `getSong()`, `getAlbum()`, `getArtist()` from Java ViewModel
        // are converted to Kotlin properties `song`, `album`, `artist`.
        // Also assuming `getLiveSong()`, etc. are `liveSong` properties.
        // And `getUserRating()` is `userRating` property.
        if (ratingViewModel.song != null) {
            ratingViewModel.liveSong.observe(lifecycleOwner) { song ->
                bind?.ratingBar?.rating = song?.userRating?.toFloat() ?: 0f
            }
        } else if (ratingViewModel.album != null) {
            ratingViewModel.liveAlbum.observe(lifecycleOwner) { album ->
                album?.let { // 'album' can be null from LiveData if not handled upstream, check explicitly
                    bind?.ratingBar?.rating = it.userRating?.toFloat() ?: 0f
                }
            }
        } else if (ratingViewModel.artist != null) {
            ratingViewModel.liveArtist.observe(lifecycleOwner) { artist ->
                // The original Java code commented out `artist.getRating()` and used `0`.
                // We reflect this by setting rating to `0f` (as RatingBar expects Float).
                bind?.ratingBar?.rating = /*artist?.rating ?:*/ 0f
            }
        }
    }
}
