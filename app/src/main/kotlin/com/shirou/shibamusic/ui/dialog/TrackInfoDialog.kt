package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaMetadata
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogTrackInfoBinding
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TrackInfoDialog(private val mediaMetadata: MediaMetadata) : DialogFragment() {
    private var bind: DialogTrackInfoBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogTrackInfoBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind!!.root)
            .setPositiveButton(R.string.track_info_dialog_positive_button) { dialog, _ -> dialog.cancel() }
            .create()
    }

    override fun onStart() {
        super.onStart()

        setTrackInfo()
        setTrackTranscodingInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setTrackInfo() {
        bind!!.trakTitleInfoTextView.text = mediaMetadata.title
        bind!!.trakArtistInfoTextView.text = mediaMetadata.artist ?:
            if (mediaMetadata.extras?.getString("type") == Constants.MEDIA_TYPE_RADIO) {
                mediaMetadata.extras?.getString("uri", getString(R.string.label_placeholder))
            } else {
                ""
            }

        mediaMetadata.extras?.let { extras ->
            CustomGlideRequest.Builder
                .from(requireContext(), extras.getString("coverArtId", ""), CustomGlideRequest.ResourceType.Song)
                .build()
                .into(bind!!.trackCoverInfoImageView)

            bind!!.titleValueSector.text = extras.getString("title", getString(R.string.label_placeholder))
            bind!!.albumValueSector.text = extras.getString("album", getString(R.string.label_placeholder))
            bind!!.artistValueSector.text = extras.getString("artist", getString(R.string.label_placeholder))
            bind!!.trackNumberValueSector.text = extras.getInt("track", 0).takeIf { it != 0 }?.toString() ?: getString(R.string.label_placeholder)
            bind!!.yearValueSector.text = extras.getInt("year", 0).takeIf { it != 0 }?.toString() ?: getString(R.string.label_placeholder)
            bind!!.genreValueSector.text = extras.getString("genre", getString(R.string.label_placeholder))
            bind!!.sizeValueSector.text = extras.getLong("size", 0).takeIf { it != 0L }?.let { MusicUtil.getReadableByteCount(it) } ?: getString(R.string.label_placeholder)
            bind!!.contentTypeValueSector.text = extras.getString("contentType", getString(R.string.label_placeholder))
            bind!!.suffixValueSector.text = extras.getString("suffix", getString(R.string.label_placeholder))
            bind!!.transcodedContentTypeValueSector.text = extras.getString("transcodedContentType", getString(R.string.label_placeholder))
            bind!!.transcodedSuffixValueSector.text = extras.getString("transcodedSuffix", getString(R.string.label_placeholder))
            bind!!.durationValueSector.text = extras.getInt("duration", 0).takeIf { it != 0 }?.let { MusicUtil.getReadableDurationString(it, false) } ?: getString(R.string.label_placeholder)
            bind!!.bitrateValueSector.text = extras.getInt("bitrate", 0).takeIf { it != 0 }?.let { "$it kbps" } ?: getString(R.string.label_placeholder)
            bind!!.samplingRateValueSector.text = extras.getInt("samplingRate", 0).takeIf { it != 0 }?.let { "$it Hz" } ?: getString(R.string.label_placeholder)
            bind!!.bitDepthValueSector.text = extras.getInt("bitDepth", 0).takeIf { it != 0 }?.let { "$it bits" } ?: getString(R.string.label_placeholder)
            bind!!.pathValueSector.text = extras.getString("path", getString(R.string.label_placeholder))
            bind!!.discNumberValueSector.text = extras.getInt("discNumber", 0).takeIf { it != 0 }?.toString() ?: getString(R.string.label_placeholder)
        }
    }

    private fun setTrackTranscodingInfo() {
        val prioritizeServerTranscoding = Preferences.isServerPrioritized()

        val transcodingExtension = MusicUtil.getTranscodingFormatPreference()
        val bitratePreference = MusicUtil.getBitratePreference().toIntOrNull() ?: 0
        val transcodingBitrate = if (bitratePreference != 0) "${bitratePreference}kbps" else "Original"

        val infoText = when {
            mediaMetadata.extras?.getString("uri", "")?.contains(Constants.DOWNLOAD_URI) == true ->
                getString(R.string.track_info_summary_downloaded_file)

            prioritizeServerTranscoding ->
                getString(R.string.track_info_summary_server_prioritized)

            transcodingExtension == "raw" && transcodingBitrate == "Original" ->
                getString(R.string.track_info_summary_original_file)

            transcodingExtension != "raw" && transcodingBitrate == "Original" ->
                getString(R.string.track_info_summary_transcoding_codec, transcodingExtension)

            transcodingExtension == "raw" && transcodingBitrate != "Original" ->
                getString(R.string.track_info_summary_transcoding_bitrate, transcodingBitrate)

            transcodingExtension != "raw" && transcodingBitrate != "Original" ->
                getString(R.string.track_info_summary_full_transcode, transcodingExtension, transcodingBitrate)

            else -> "" // Should not be reached if Java logic covers all cases.
        }

        bind!!.trakTranscodingInfoTextView.text = infoText
    }
}
