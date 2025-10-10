package com.shirou.shibamusic.ui.activity

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.shirou.shibamusic.App
import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.R
import com.shirou.shibamusic.broadcast.receiver.ConnectivityStatusBroadcastReceiver
import com.shirou.shibamusic.databinding.ActivityMainBinding
import com.shirou.shibamusic.github.utils.UpdateUtil
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.ui.activity.base.BaseActivity
import com.shirou.shibamusic.ui.dialog.ConnectionAlertDialog
import com.shirou.shibamusic.ui.dialog.GithubShibaMusicUpdateDialog
import com.shirou.shibamusic.ui.dialog.ServerUnreachableDialog
import com.shirou.shibamusic.ui.fragment.PlayerBottomSheetFragment
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.DynamicColors
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException
import kotlin.math.max
import kotlin.math.min

@UnstableApi
class MainActivity : BaseActivity() {
    private val TAG = "MainActivityLogs"

    var bind: ActivityMainBinding? = null
    private lateinit var mainViewModel: MainViewModel

    private lateinit var fragmentManager: FragmentManager
    private var navHostFragment: NavHostFragment? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    lateinit var navController: NavController
        private set
    private var navControllerInitialized = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var connectivityStatusBroadcastReceiver: ConnectivityStatusBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "MainActivity onCreate started")
    installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)

        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind!!.root)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        connectivityStatusBroadcastReceiver = ConnectivityStatusBroadcastReceiver(this)
        connectivityStatusReceiverManager(true)

        // Timeout de segurança para tentar recuperação da UI caso a navegação não esteja pronta
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Safety timeout reached, checking if app is visible")
            if (::navController.isInitialized && navController.currentDestination != null) {
                Log.d(TAG, "App appears to be working normally")
            } else {
                Log.w(TAG, "App may have failed to initialize properly, attempting recovery")
                try {
                    init()
                } catch (e: Exception) {
                    Log.e(TAG, "Recovery attempt failed", e)
                }
            }
        }, 5000)

        init()
        checkConnectionType()
        getOpenSubsonicExtensions()
        checkShibaMusicUpdate()
        Log.d(TAG, "MainActivity onCreate completed")
    }

    override fun onStart() {
        super.onStart()
        initService()
    }

    override fun onResume() {
        super.onResume()
        pingServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityStatusReceiverManager(false)
        bind = null
    }

    override fun onBackPressed() {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            collapseBottomSheetDelayed()
        } else {
            super.onBackPressed()
        }
    }

    fun init() {
        Log.d(TAG, "MainActivity init() started")
        fragmentManager = supportFragmentManager

        try {
            initBottomSheet()
            initNavigation()

            if (Preferences.getPassword() != null || (Preferences.getToken() != null && Preferences.getSalt() != null)) {
                Log.d(TAG, "User credentials found, going to home")
                goFromLogin()
            } else {
                Log.d(TAG, "No user credentials, going to login")
                goToLogin()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during MainActivity initialization", e)
            try {
                goToLogin()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Critical error in MainActivity init fallback", fallbackError)
            }
        }
        Log.d(TAG, "MainActivity init() completed")
    }

    // BOTTOM SHEET/NAVIGATION
    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.player_bottom_sheet)!!)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
        fragmentManager.beginTransaction()
            .replace(R.id.player_bottom_sheet, PlayerBottomSheetFragment(), "PlayerBottomSheet").commit()

        checkBottomSheetAfterStateChanged()
    }

    fun setBottomSheetInPeek(isVisible: Boolean) {
        bottomSheetBehavior.state = if (isVisible) BottomSheetBehavior.STATE_COLLAPSED else BottomSheetBehavior.STATE_HIDDEN
    }

    fun setBottomSheetVisibility(visibility: Boolean) {
        findViewById<View>(R.id.player_bottom_sheet)!!.visibility = if (visibility) View.VISIBLE else View.GONE
    }

    private fun checkBottomSheetAfterStateChanged() {
        Handler(Looper.getMainLooper()).postDelayed({
            setBottomSheetInPeek(mainViewModel.isQueueLoaded())
        }, 100)
    }

    fun collapseBottomSheetDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    fun expandBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setBottomSheetDraggableState(isDraggable: Boolean) {
        bottomSheetBehavior.isDraggable = isDraggable
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        private var navigationHeight: Int = 0

        override fun onStateChanged(@NonNull view: View, state: Int) {
            val playerBottomSheetFragment = supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment

            when (state) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    resetMusicSession()
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    playerBottomSheetFragment?.goBackToFirstPage()
                }
                BottomSheetBehavior.STATE_SETTLING,
                BottomSheetBehavior.STATE_EXPANDED,
                BottomSheetBehavior.STATE_DRAGGING,
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    // no-op
                }
            }
        }

        override fun onSlide(@NonNull view: View, slideOffset: Float) {
            animateBottomSheet(slideOffset)
            if (navigationHeight == 0) {
                navigationHeight = bind?.bottomNavigation?.height ?: 0
            }
            animateBottomNavigation(slideOffset, navigationHeight)
        }
    }

    private fun animateBottomSheet(slideOffset: Float) {
        (supportFragmentManager.findFragmentByTag("PlayerBottomSheet") as? PlayerBottomSheetFragment)?.playerHeader?.let { playerHeader ->
            val condensedSlideOffset = max(0.0f, min(0.2f, slideOffset - 0.2f)) / 0.2f
            playerHeader.alpha = 1 - condensedSlideOffset
            playerHeader.visibility = if (condensedSlideOffset > 0.99) View.GONE else View.VISIBLE
        }
    }

    private fun animateBottomNavigation(slideOffset: Float, navigationHeight: Int) {
        if (slideOffset < 0) return
        val slideY = navigationHeight - navigationHeight * (1 - slideOffset)
        bind?.bottomNavigation?.translationY = slideY
        }

    private fun initNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        navControllerInitialized = false
        navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

        navHostFragment ?: run {
            Log.e(TAG, "NavHostFragment not found! This could cause blank screen.")
            return
        }

        val controller = navHostFragment!!.navController
        navController = controller
        navControllerInitialized = true

        controller.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navigation destination changed to: ${destination.label}")
            if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED &&
                (destination.id == R.id.homeFragment ||
                        destination.id == R.id.libraryFragment ||
                        destination.id == R.id.downloadFragment)
            ) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        NavigationUI.setupWithNavController(bottomNavigationView, controller)
        Log.d(TAG, "Navigation setup completed successfully")
    }

    private fun isNavControllerReady(): Boolean = ::navController.isInitialized && navControllerInitialized

    fun requireNavController(): NavController {
        check(isNavControllerReady()) { "NavController has not been initialized yet" }
        return navController
    }

    fun navControllerOrNull(): NavController? = if (isNavControllerReady()) navController else null

    fun setBottomNavigationBarVisibility(visibility: Boolean) {
        bottomNavigationView.visibility = if (visibility) View.VISIBLE else View.GONE
    }

    fun navigateIfPossible(@IdRes actionId: Int, args: Bundle? = null) {
        if (!isNavControllerReady()) {
            Log.w(TAG, "navigateIfPossible skipped: NavController not ready (actionId=$actionId)")
            return
        }
        try {
            navController.navigate(actionId, args)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Navigation failed for actionId=$actionId", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Navigation state error for actionId=$actionId", e)
        }
    }

    fun navigateUpIfPossible(): Boolean {
        if (!isNavControllerReady()) {
            Log.w(TAG, "navigateUpIfPossible skipped: NavController not ready")
            return false
        }
        return try {
            navController.navigateUp()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "navigateUpIfPossible failed", e)
            false
        }
    }

    private fun initService() {
        val mediaBrowserFuture = mediaBrowserListenableFuture
        MediaManager.check(mediaBrowserFuture)

        mediaBrowserFuture.addListener({
            try {
                mediaBrowserFuture.get().addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && ::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                            setBottomSheetInPeek(true)
                        }
                    }
                })
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun goToLogin() {
        Log.d(TAG, "Navigating to login screen")
        if (::bottomSheetBehavior.isInitialized) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        setBottomNavigationBarVisibility(false)
        setBottomSheetVisibility(false)

        if (!isNavControllerReady()) {
            Log.e(TAG, "NavController not initialized in goToLogin()")
            return
        }

        try {
            val controller = requireNavController()
            val currentDestinationId = controller.currentDestination?.id ?: run {
                Log.e(TAG, "Current destination is null in goToLogin()")
                return
            }

            Log.d(TAG, "Current destination: $currentDestinationId")

            when (currentDestinationId) {
                R.id.landingFragment -> controller.navigate(R.id.action_landingFragment_to_loginFragment)
                R.id.homeFragment -> controller.navigate(R.id.action_homeFragment_to_loginFragment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login", e)
        }
    }

    private fun goToHome() {
        Log.d(TAG, "Navigating to home screen")
        bottomNavigationView.visibility = View.VISIBLE

        if (!isNavControllerReady()) {
            Log.e(TAG, "NavController not initialized in goToHome()")
            return
        }

        try {
            val controller = requireNavController()
            val currentDestinationId = controller.currentDestination?.id ?: run {
                Log.e(TAG, "Current destination is null in goToHome()")
                return
            }

            Log.d(TAG, "Current destination: $currentDestinationId")

            when (currentDestinationId) {
                R.id.landingFragment -> controller.navigate(R.id.action_landingFragment_to_homeFragment)
                R.id.loginFragment -> controller.navigate(R.id.action_loginFragment_to_homeFragment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to home", e)
        }
    }

    fun goFromLogin() {
        setBottomSheetInPeek(mainViewModel.isQueueLoaded())
        goToHome()
    }

    fun quit() {
        resetUserSession()
        resetMusicSession()
        resetViewModel()
        goToLogin()
    }

    private fun resetUserSession() {
        Preferences.setServerId(null)
        Preferences.setSalt(null)
        Preferences.setToken(null)
        Preferences.setPassword(null)
        Preferences.setServer(null)
        Preferences.setLocalAddress(null)
        Preferences.setUser(null)

        // Reset de flags e preferências
        Preferences.setOpenSubsonic(false)
        Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_100)
        Preferences.setSkipSilenceMode(false)
        Preferences.setDataSavingMode(false)
        Preferences.setStarredSyncEnabled(false)
        Preferences.setStarredAlbumsSyncEnabled(false)
    }

    private fun resetMusicSession() {
        MediaManager.reset(mediaBrowserListenableFuture)
    }

    private fun hideMusicSession() {
        MediaManager.hide(mediaBrowserListenableFuture)
    }

    private fun resetViewModel() {
        viewModelStore.clear()
    }

    // CONNECTION
    private fun connectivityStatusReceiverManager(isActive: Boolean) {
        if (isActive) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(connectivityStatusBroadcastReceiver, filter)
        } else {
            unregisterReceiver(connectivityStatusBroadcastReceiver)
        }
    }

    private fun pingServer() {
        if (Preferences.getToken() == null) return

        if (Preferences.isInUseServerAddressLocal()) {
            mainViewModel.ping().observe(this) { subsonicResponse ->
                if (subsonicResponse == null) {
                    Preferences.setServerSwitchableTimer()
                    Preferences.switchInUseServerAddress()
                    App.refreshSubsonicClient()
                    pingServer()
                } else {
                    Preferences.setOpenSubsonic(subsonicResponse.openSubsonic == true)
                }
            }
        } else {
            if (Preferences.isServerSwitchable()) {
                Preferences.setServerSwitchableTimer()
                Preferences.switchInUseServerAddress()
                App.refreshSubsonicClient()
                pingServer()
            } else {
                mainViewModel.ping().observe(this) { subsonicResponse ->
                    if (subsonicResponse == null) {
                        if (Preferences.showServerUnreachableDialog()) {
                            ServerUnreachableDialog().show(supportFragmentManager, null)
                        }
                    } else {
                        Preferences.setOpenSubsonic(subsonicResponse.openSubsonic == true)
                    }
                }
            }
        }
    }

    private fun getOpenSubsonicExtensions() {
        if (Preferences.getToken() != null) {
            mainViewModel.getOpenSubsonicExtensions().observe(this) { openSubsonicExtensions ->
                if (openSubsonicExtensions != null) {
                    Preferences.setOpenSubsonicExtensions(openSubsonicExtensions)
                }
            }
        }
    }

    private fun checkShibaMusicUpdate() {
        if (BuildConfig.FLAVOR == "ShibaMusic" && Preferences.showShibaMusicUpdateDialog()) {
            mainViewModel.checkShibaMusicUpdate().observe(this) { latestRelease ->
                if (latestRelease != null && UpdateUtil.showUpdateDialog(latestRelease)) {
                    GithubShibaMusicUpdateDialog(latestRelease).show(supportFragmentManager, null)
                }
            }
        }
    }

    private fun checkConnectionType() {
        try {
            if (Preferences.isWifiOnly()) {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

                connectivityManager ?: run {
                    Log.w(TAG, "ConnectivityManager is null")
                    return
                }

                if (!com.shirou.shibamusic.util.NetworkUtil.isWifiConnected()) {
                    ConnectionAlertDialog().show(supportFragmentManager, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection type", e)
        }
    }
}
