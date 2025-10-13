package com.shirou.shibamusic.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.github.models.LatestRelease
import com.shirou.shibamusic.github.utils.UpdateUtil
import com.shirou.shibamusic.interfaces.SystemCallback
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.OpenSubsonicExtension
import com.shirou.shibamusic.subsonic.models.ResponseStatus
import com.shirou.shibamusic.subsonic.models.SubsonicResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SystemRepository {
    fun checkUserCredential(callback: SystemCallback) {
        App.getSubsonicClientInstance(false)
            .systemClient
            .ping()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.let { subsonicResponse ->
                        when (subsonicResponse.status) {
                            ResponseStatus.FAILED -> {
                                val error = subsonicResponse.error
                                callback.onError(Exception("${error?.code} - ${error?.message}"))
                            }
                            ResponseStatus.OK -> {
                                val rawRequest = response.raw().request
                                val url = rawRequest.url
                                val password = url.queryParameter("p")
                                val token = url.queryParameter("t")
                                val salt = url.queryParameter("s")
                                callback.onSuccess(password!!, token!!, salt!!) // !! used as Java code implies non-null expectation
                            }
                            else -> {
                                callback.onError(Exception("Empty response")) // Matches Java's 'else' for non-OK/FAILED status in a valid body
                            }
                        }
                    } ?: run {
                        callback.onError(Exception(response.code().toString())) // Matches Java's 'else' for null body or null subsonicResponse
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    callback.onError(Exception(t.message))
                }
            })
    }

    fun ping(): MutableLiveData<SubsonicResponse?> {
        val pingResult = MutableLiveData<SubsonicResponse?>()

        App.getSubsonicClientInstance(false)
            .systemClient
            .ping()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        pingResult.postValue(response.body()?.subsonicResponse)
                    } else {
                        pingResult.postValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    pingResult.postValue(null)
                }
            })

        return pingResult
    }

    fun getOpenSubsonicExtensions(): MutableLiveData<List<OpenSubsonicExtension>?> {
        val extensionsResult = MutableLiveData<List<OpenSubsonicExtension>?>()

        App.getSubsonicClientInstance(false)
            .systemClient
            .getOpenSubsonicExtensions()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        extensionsResult.postValue(response.body()?.subsonicResponse?.openSubsonicExtensions)
                    }
                    // Java code did not post null if not successful, maintaining this semantic.
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    extensionsResult.postValue(null)
                }
            })

        return extensionsResult
    }

    fun checkShibaMusicUpdate(context: Context): MutableLiveData<LatestRelease?> {
        val latestRelease = MutableLiveData<LatestRelease?>()

        App.getGithubClientInstance()
            .releaseClient
            .getLatestRelease()
            .enqueue(object : Callback<LatestRelease> {
                override fun onResponse(call: Call<LatestRelease>, response: Response<LatestRelease>) {
                    if (response.isSuccessful && response.body() != null) {
                        val release = response.body()!!
                        latestRelease.postValue(release)

                        if (UpdateUtil.showUpdateDialog(release)) {
                            showUpdateNotification(context, release)
                        }
                    }
                }

                override fun onFailure(call: Call<LatestRelease>, t: Throwable) {
                    latestRelease.postValue(null)
                }
            })

        return latestRelease
    }

    private fun showUpdateNotification(context: Context, release: LatestRelease) {
        val channelId = "shibamusic_update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("New version available")
            .setContentText("Version ${release.tagName} is available.")
            .setSmallIcon(R.drawable.shiba_vector)
            .build()

        notificationManager.notify(1, notification)
    }
}
