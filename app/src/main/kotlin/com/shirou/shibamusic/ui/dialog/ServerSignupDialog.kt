package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogServerSignupBinding
import com.shirou.shibamusic.model.Server
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.viewmodel.LoginViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class ServerSignupDialog : DialogFragment() {

    private var _binding: DialogServerSignupBinding? = null
    // This property is only valid between onCreateDialog and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var loginViewModel: LoginViewModel

    private var serverName: String = ""
    private var username: String = ""
    private var password: String = ""
    private var server: String? = null
    private var localAddress: String? = null
    private var lowSecurity: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogServerSignupBinding.inflate(layoutInflater)

        loginViewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]

        return MaterialAlertDialogBuilder(requireActivity()).apply {
            setView(binding.root)
            setTitle(R.string.server_signup_dialog_title)
            setNeutralButton(R.string.server_signup_dialog_neutral_button) { _, _ -> }
            setPositiveButton(R.string.server_signup_dialog_positive_button) { _, _ -> }
            setNegativeButton(R.string.server_signup_dialog_negative_button) { dialog, _ -> dialog.cancel() }
        }.create()
    }

    override fun onStart() {
        super.onStart()

        setServerInfo()
        setButtonAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setServerInfo() {
        arguments?.let {
            // getParcelable with reified type parameter is preferred in Kotlin
            loginViewModel.serverToEdit = it.getParcelable("server_object")

            loginViewModel.serverToEdit?.let { serverToEdit ->
                binding.serverNameTextView.setText(serverToEdit.serverName)
                binding.usernameTextView.setText(serverToEdit.username)
                binding.passwordTextView.setText("") // Always clear password for security
                binding.serverTextView.setText(serverToEdit.address ?: "")
                binding.localAddressTextView.setText(serverToEdit.localAddress ?: "")
                binding.lowSecurityCheckbox.isChecked = serverToEdit.isLowSecurity
            }
        } ?: run {
            loginViewModel.serverToEdit = null
        }
    }

    private fun setButtonAction() {
        // Cast to AlertDialog is safe here as MaterialAlertDialogBuilder creates an AlertDialog
        val alertDialog = requireDialog() as AlertDialog

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateInput()) {
                saveServerPreference()
                requireDialog().dismiss()
            }
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            Toast.makeText(requireContext(), R.string.server_signup_dialog_action_delete_toast, Toast.LENGTH_SHORT).show()
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnLongClickListener {
            // Original Java code passes null, maintaining semantic equivalence.
            loginViewModel.deleteServer(null)
            requireDialog().dismiss()
            true // Indicate that the long click was consumed
        }
    }

    private fun validateInput(): Boolean {
        // Assigning to member variables
        serverName = binding.serverNameTextView.text.toString().trim()
        username = binding.usernameTextView.text.toString().trim()
        
        val passwordText = binding.passwordTextView.text.toString()
        password = if (binding.lowSecurityCheckbox.isChecked) {
            MusicUtil.passwordHexEncoding(passwordText)
        } else {
            passwordText
        }

        // Use takeIf to set to null if blank, matching Java's ternary logic
        server = binding.serverTextView.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
        localAddress = binding.localAddressTextView.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
        lowSecurity = binding.lowSecurityCheckbox.isChecked

        if (serverName.isEmpty()) {
            binding.serverNameTextView.error = getString(R.string.error_required)
            return false
        }

        if (username.isEmpty()) {
            binding.usernameTextView.error = getString(R.string.error_required)
            return false
        }

        // isNullOrEmpty correctly handles String?
        if (server.isNullOrEmpty()) {
            binding.serverTextView.error = getString(R.string.error_required)
            return false
        }

        // Only check localAddress if it's not null and not empty
        localAddress?.let { address ->
            if (!address.matches("^https?://(.*)".toRegex())) {
                binding.localAddressTextView.error = getString(R.string.error_server_prefix)
                return false
            }
        }
        
        // At this point, 'server' is guaranteed to be non-null due to the previous check.
        if (!server!!.matches("^https?://(.*)".toRegex())) {
            binding.serverTextView.error = getString(R.string.error_server_prefix)
            return false
        }

        return true
    }

    private fun saveServerPreference() {
        val serverID = loginViewModel.serverToEdit?.serverId ?: UUID.randomUUID().toString()
        val address = requireNotNull(this.server) { "Server address must be set before saving preferences" }
        
        loginViewModel.addServer(
            Server(
                serverID,
                this.serverName,
                this.username,
                this.password,
                address,
                this.localAddress,
                System.currentTimeMillis(),
                this.lowSecurity
            )
        )
    }

    companion object {
        private const val TAG = "ServerSignupDialog"
    }
}
