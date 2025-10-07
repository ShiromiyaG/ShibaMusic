package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogPlaylistChooserBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.ui.adapter.PlaylistDialogHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.viewmodel.PlaylistChooserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.ArrayList

class PlaylistChooserDialog : DialogFragment(), ClickCallback {
    private var bind: DialogPlaylistChooserBinding? = null
    private lateinit var playlistChooserViewModel: PlaylistChooserViewModel
    private lateinit var playlistDialogHorizontalAdapter: PlaylistDialogHorizontalAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogPlaylistChooserBinding.inflate(layoutInflater)

        playlistChooserViewModel = ViewModelProvider(requireActivity())[PlaylistChooserViewModel::class.java]

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind!!.root)
            .setTitle(R.string.playlist_chooser_dialog_title)
            .setNeutralButton(R.string.playlist_chooser_dialog_neutral_button) { _, _ -> /* No action */ }
            .setNegativeButton(R.string.playlist_chooser_dialog_negative_button) { dialog, _ -> dialog.cancel() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    override fun onStart() {
        super.onStart()

        initPlaylistView()
        setSongInfo()
        setButtonAction()
    }

    private fun setSongInfo() {
        playlistChooserViewModel.songsToAdd = requireArguments()
            .getParcelableArrayList<Child>(Constants.TRACKS_OBJECT)
            ?.toMutableList()
            ?: mutableListOf()
    }

    private fun setButtonAction() {
        val alertDialog = requireDialog() as androidx.appcompat.app.AlertDialog
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList(playlistChooserViewModel.songsToAdd))
            }

            // Assuming PlaylistEditorDialog constructor can accept null or has a default/no-arg constructor
            val editorDialog = PlaylistEditorDialog()
            editorDialog.arguments = bundle
            editorDialog.show(requireActivity().supportFragmentManager, null)

            requireDialog().dismiss()
        }
    }

    private fun initPlaylistView() {
        // Initialize adapter before setting it to RecyclerView
        playlistDialogHorizontalAdapter = PlaylistDialogHorizontalAdapter(this)

        bind!!.playlistDialogRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = playlistDialogHorizontalAdapter
        }

        playlistChooserViewModel.getPlaylistList(requireActivity()).observe(requireActivity()) { playlists ->
            playlists?.let {
                if (it.isNotEmpty()) {
                    bind?.noPlaylistsCreatedTextView?.visibility = View.GONE
                    bind?.playlistDialogRecyclerView?.visibility = View.VISIBLE
                    playlistDialogHorizontalAdapter.setItems(it)
                } else {
                    bind?.noPlaylistsCreatedTextView?.visibility = View.VISIBLE
                    bind?.playlistDialogRecyclerView?.visibility = View.GONE
                }
            }
        }
    }

    override fun onPlaylistClick(bundle: Bundle) {
        if (playlistChooserViewModel.songsToAdd.isNotEmpty()) {
            val playlist: Playlist = requireNotNull(bundle.getParcelable(Constants.PLAYLIST_OBJECT)) {
                "Playlist object cannot be null in bundle for onPlaylistClick"
            }
            playlistChooserViewModel.addSongsToPlaylist(playlist.id)
            dismiss()
        } else {
            Toast.makeText(requireContext(), R.string.playlist_chooser_dialog_toast_add_failure, Toast.LENGTH_SHORT).show()
        }
    }
}
