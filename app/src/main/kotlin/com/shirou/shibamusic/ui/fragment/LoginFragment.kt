package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.ui.adapter.ServerAdapter
import com.shirou.shibamusic.databinding.FragmentLoginBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.SystemCallback
import com.shirou.shibamusic.model.Server
import com.shirou.shibamusic.repository.SystemRepository
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.dialog.ServerSignupDialog
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.LoginViewModel

@UnstableApi
class LoginFragment : Fragment(), ClickCallback {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var serverAdapter: ServerAdapter

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(@NonNull menu: Menu, @NonNull inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.login_page_menu, menu)
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity
        loginViewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root

        initAppBar()
        initServerListView()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAppBar() {
        activity.setSupportActionBar(binding.toolbar)

        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if ((binding.serverInfoSector.height + verticalOffset) < (2 * ViewCompat.getMinimumHeight(binding.toolbar))) {
                binding.toolbar.setTitle(R.string.login_title)
            } else {
                binding.toolbar.setTitle(R.string.empty_string)
            }
        }
    }

    private fun initServerListView() {
        binding.serverListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.serverListRecyclerView.setHasFixedSize(true)

        serverAdapter = ServerAdapter(this)
        binding.serverListRecyclerView.adapter = serverAdapter
        loginViewModel.serverList.observe(viewLifecycleOwner) { servers ->
            val items = servers ?: emptyList()
            _binding?.let { currentBinding ->
                if (items.isNotEmpty()) {
                    currentBinding.noServerAddedTextView.visibility = View.GONE
                    currentBinding.serverListRecyclerView.visibility = View.VISIBLE
                    serverAdapter.setItems(items)
                } else {
                    currentBinding.noServerAddedTextView.visibility = View.VISIBLE
                    currentBinding.serverListRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add) {
            val dialog = ServerSignupDialog()
            dialog.show(activity.supportFragmentManager, null)
            return true
        }

        return false
    }

    override fun onServerClick(bundle: Bundle) {
        val server = BundleCompat.getParcelable(bundle, "server_object", Server::class.java)
            ?: return
        saveServerPreference(server.serverId, server.address, server.localAddress, server.username, server.password, server.isLowSecurity)

        val systemRepository = SystemRepository()
        systemRepository.checkUserCredential(object : SystemCallback {
            override fun onError(exception: Exception) {
                Preferences.switchInUseServerAddress()
                resetServerPreference()
                Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(password: String, token: String, salt: String) {
                activity.goFromLogin()
            }
        })
    }

    override fun onServerLongClick(bundle: Bundle) {
        val dialog = ServerSignupDialog()
        dialog.arguments = bundle
        dialog.show(activity.supportFragmentManager, null)
    }

    private fun saveServerPreference(serverId: String?, server: String?, localAddress: String?, user: String?, password: String?, isLowSecurity: Boolean) {
    Preferences.setServerId(serverId)
    Preferences.setServer(server)
    Preferences.setLocalAddress(localAddress)
    Preferences.setUser(user)
    Preferences.setPassword(password)
    Preferences.setLowSecurity(isLowSecurity)

        App.getSubsonicClientInstance(true)
    }

    private fun resetServerPreference() {
    Preferences.setServerId(null)
    Preferences.setServer(null)
    Preferences.setUser(null)
    Preferences.setPassword(null)
    Preferences.setToken(null)
    Preferences.setSalt(null)
    Preferences.setLowSecurity(false)

        App.getSubsonicClientInstance(true)
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
