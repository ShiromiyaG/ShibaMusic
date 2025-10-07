package com.shirou.shibamusic.ui.activity.base

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.service.DownloaderService
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.dialog.BatteryOptimizationDialog
import com.shirou.shibamusic.util.Flavors
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
open class BaseActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseActivity"
    }

    private lateinit var _mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    // Expose the ListenableFuture as a public read-only property, mirroring the Java getter.
    val mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>
        get() = _mediaBrowserListenableFuture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Flavors.initializeCastContext(this)
        initializeDownloader()
        checkBatteryOptimization()
        checkPermission()
        checkAlwaysOnDisplay()
    }

    override fun onStart() {
        super.onStart()
        setNavigationBarColor()
        initializeBrowser()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    private fun checkBatteryOptimization() {
        if (detectBatteryOptimization() && Preferences.askForOptimization()) {
            showBatteryOptimizationDialog()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun checkAlwaysOnDisplay() {
        if (Preferences.isDisplayAlwaysOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun detectBatteryOptimization(): Boolean {
        val packageName = packageName
        // The Java code implicitly assumes getSystemService returns a non-null PowerManager.
        // We reflect this by casting directly, which will throw if null or wrong type.
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showBatteryOptimizationDialog() {
        val dialog = BatteryOptimizationDialog()
        dialog.show(supportFragmentManager, null)
    }

    private fun initializeBrowser() {
        _mediaBrowserListenableFuture = MediaBrowser.Builder(this, SessionToken(this, ComponentName(this, MediaService::class.java))).buildAsync()
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(_mediaBrowserListenableFuture)
    }

    private fun initializeDownloader() {
        try {
            DownloadService.start(this, DownloaderService::class.java)
        } catch (e: IllegalStateException) {
            DownloadService.startForeground(this, DownloaderService::class.java)
        }
    }

    private fun setNavigationBarColor() {
    window.navigationBarColor = SurfaceColors.getColorForElevation(this, 8f)
    window.statusBarColor = SurfaceColors.getColorForElevation(this, 0f)
    }
}
