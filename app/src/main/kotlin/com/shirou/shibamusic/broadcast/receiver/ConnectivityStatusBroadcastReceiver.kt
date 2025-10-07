package com.shirou.shibamusic.broadcast.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View

import androidx.media3.common.util.UnstableApi
import kotlin.OptIn

import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.util.NetworkUtil

@OptIn(UnstableApi::class)
class ConnectivityStatusBroadcastReceiver(private val activity: MainActivity) : BroadcastReceiver() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingUpdate: Runnable? = null

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            Log.d(TAG, "Connectivity change detected")

            pendingUpdate?.let { callback ->
                handler.removeCallbacks(callback)
            }

            val updateTask = Runnable {
                try {
                    val isOffline = NetworkUtil.isOffline()
                    Log.d(TAG, "Network status - Offline: $isOffline")

                    activity.bind?.offlineModeTextView?.let { textView ->
                        if (isOffline) {
                            textView.visibility = View.VISIBLE
                            Log.d(TAG, "Showing offline mode indicator")
                        } else {
                            textView.visibility = View.GONE
                            Log.d(TAG, "Hiding offline mode indicator")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating connectivity status", e)
                }
            }

            pendingUpdate = updateTask

            handler.postDelayed(updateTask, 1000L)
        }
    }

    companion object {
        private const val TAG = "ConnectivityReceiver"
    }
}
