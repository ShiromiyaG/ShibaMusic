package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogPlaylistEditorBinding
import com.shirou.shibamusic.interfaces.PlaylistCallback
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.ui.adapter.PlaylistDialogSongHorizontalAdapter
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.PlaylistEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Collections

class PlaylistEditorDialog(private val playlistCallback: PlaylistCallback? = null) : DialogFragment() {
    private var bind: DialogPlaylistEditorBinding? = null
    private lateinit var playlistEditorViewModel: PlaylistEditorViewModel

    private var playlistName: String = ""
    private lateinit var playlistDialogSongHorizontalAdapter: PlaylistDialogSongHorizontalAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogPlaylistEditorBinding.inflate(layoutInflater)

        playlistEditorViewModel = ViewModelProvider(requireActivity())[PlaylistEditorViewModel::class.java]

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind!!.root)
            .setTitle(R.string.playlist_editor_dialog_title)
            .setPositiveButton(R.string.playlist_editor_dialog_positive_button) { _, _ -> }
            .setNeutralButton(R.string.playlist_editor_dialog_neutral_button) { dialog, _ -> dialog.cancel() }
            .setNegativeButton(R.string.playlist_editor_dialog_negative_button) { dialog, _ -> dialog.cancel() }
            .create()
    }

    override fun onStart() {
        super.onStart()

        setParameterInfo()
        setButtonAction()
        initSongsView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setParameterInfo() {
        val arguments = requireArguments()
        bind?.apply {
            if (arguments.getParcelableArrayList<Child>(Constants.TRACKS_OBJECT) != null) {
                playlistEditorViewModel.songsToAdd = arguments.getParcelableArrayList(Constants.TRACKS_OBJECT)
                playlistEditorViewModel.playlistToEdit = null
            } else if (arguments.getParcelable<Playlist>(Constants.PLAYLIST_OBJECT) != null) {
                playlistEditorViewModel.songsToAdd = null
                playlistEditorViewModel.playlistToEdit = arguments.getParcelable(Constants.PLAYLIST_OBJECT)

                playlistEditorViewModel.playlistToEdit?.let { playlistToEdit ->
                    playlistNameTextView.setText(playlistToEdit.name.orEmpty())
                }
            }
        }
    }

    private fun setButtonAction() {
        val alertDialog = requireDialog() as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateInput()) {
                if (playlistEditorViewModel.songsToAdd != null) {
                    playlistEditorViewModel.createPlaylist(playlistName)
                } else if (playlistEditorViewModel.playlistToEdit != null) {
                    playlistEditorViewModel.updatePlaylist(playlistName)
                }
                dialogDismiss()
            }
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            Toast.makeText(requireContext(), R.string.playlist_editor_dialog_action_delete_toast, Toast.LENGTH_SHORT).show()
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnLongClickListener {
            playlistEditorViewModel.deletePlaylist()
            dialogDismiss()
            false
        }

        bind?.playlistShareButton?.setOnClickListener {
            playlistEditorViewModel.sharePlaylist().observe(requireActivity()) { sharedPlaylist ->
                val shareUrl = sharedPlaylist?.url
                if (!shareUrl.isNullOrEmpty()) {
                    val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(getString(R.string.app_name), shareUrl)
                    clipboardManager.setPrimaryClip(clipData)
                }
            }
        }

        bind?.playlistShareButton?.visibility = if (Preferences.isSharingEnabled()) View.VISIBLE else View.GONE
    }

    private fun initSongsView() {
        bind?.playlistSongRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)

            playlistDialogSongHorizontalAdapter = PlaylistDialogSongHorizontalAdapter()
            adapter = playlistDialogSongHorizontalAdapter
        }

        playlistEditorViewModel.playlistSongLiveList.observe(viewLifecycleOwner) { songs ->
            songs?.let { playlistDialogSongHorizontalAdapter.setItems(it) }
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            var originalPosition = -1
            var fromPosition = -1
            var toPosition = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1)
                    originalPosition = viewHolder.bindingAdapterPosition

                fromPosition = viewHolder.bindingAdapterPosition
                toPosition = target.bindingAdapterPosition

                Collections.swap(playlistDialogSongHorizontalAdapter.items, fromPosition, toPosition)
                recyclerView.adapter!!.notifyItemMoved(fromPosition, toPosition)

                return false
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                /*
                 * Qui vado a riscivere tutta la table Queue, quando teoricamente potrei solo swappare l'ordine degli elementi interessati
                 * Nel caso la coda contenesse parecchi brani, potrebbero verificarsi rallentamenti pesanti
                 */
                playlistEditorViewModel.orderPlaylistSongLiveListAfterSwap(playlistDialogSongHorizontalAdapter.items)

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                playlistEditorViewModel.removeFromPlaylistSongLiveList(viewHolder.bindingAdapterPosition)
                bind!!.playlistSongRecyclerView.adapter!!.notifyItemRemoved(viewHolder.bindingAdapterPosition)
            }
        }).attachToRecyclerView(bind!!.playlistSongRecyclerView)
    }

    private fun validateInput(): Boolean {
        playlistName = bind!!.playlistNameTextView.text.toString().trim()

        if (playlistName.isEmpty()) {
            bind!!.playlistNameTextView.error = getString(R.string.error_required)
            return false
        }

        return true
    }

    private fun dialogDismiss() {
        dialog?.dismiss()
        playlistCallback?.onDismiss()
    }
}
