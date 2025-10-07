package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Share
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SharingRepository {
    fun getShares(): MutableLiveData<List<Share>> {
        val shares = MutableLiveData<List<Share>>(emptyList())

        App.getSubsonicClientInstance(false)
            .sharingClient
            .getShares()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()
                            ?.subsonicResponse
                            ?.shares
                            ?.shares
                            ?.let { sharesList ->
                                shares.postValue(sharesList)
                            }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure if needed, shares remains empty or null depending on requirement.
                }
            })

        return shares
    }

    fun createShare(id: String, description: String?, expires: Long?): MutableLiveData<Share?> {
        val share = MutableLiveData<Share?>(null)

        App.getSubsonicClientInstance(false)
            .sharingClient
            .createShare(id, description, expires)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()
                            ?.subsonicResponse
                            ?.shares
                            ?.shares
                            ?.getOrNull(0)
                            ?.let { firstShare ->
                                share.postValue(firstShare)
                            } ?: run {
                                share.postValue(null)
                            }
                    } else {
                        share.postValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    share.postValue(null)
                }
            })

        return share
    }

    fun updateShare(id: String, description: String?, expires: Long?) {
        App.getSubsonicClientInstance(false)
            .sharingClient
            .updateShare(id, description, expires)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // No action needed on successful update according to original Java code
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No action needed on failure according to original Java code
                }
            })
    }

    fun deleteShare(id: String) {
        App.getSubsonicClientInstance(false)
            .sharingClient
            .deleteShare(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // No action needed on successful delete according to original Java code
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No action needed on failure according to original Java code
                }
            })
    }
}
