package com.shirou.shibamusic.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.shirou.shibamusic.App

object NetworkUtil {
    private const val TAG = "NetworkUtil"

    fun isOffline(): Boolean {
        return try {
            val connectivityManager = App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager == null) {
                Log.w(TAG, "ConnectivityManager is null, assuming offline")
                return true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: run {
                    Log.d(TAG, "No active network found")
                    return true
                }

                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: run {
                    Log.d(TAG, "No network capabilities found")
                    return true
                }

                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (!hasInternet) {
                    Log.d(TAG, "Network does not have internet capability")
                    return true
                }

                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (!isValidated) {
                    Log.w(TAG, "Network not validated, but allowing connection attempt")
                }

                Log.d(TAG, "Network available - Internet: $hasInternet, Validated: $isValidated")
                false
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                val isConnected = networkInfo?.isConnected ?: false
                Log.d(TAG, "Legacy network check - Connected: $isConnected")
                !isConnected
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error checking network status, assuming offline", exception)
            true
        }
    }

    fun isConnected(): Boolean = !isOffline()

    fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error checking WiFi status", exception)
            false
        }
    }
}
