package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PodcastRepository {

    companion object {
        private const val TAG = "PodcastRepository"
    }

    fun getPodcastChannels(includeEpisodes: Boolean, channelId: String?): MutableLiveData<List<PodcastChannel>> {
        val livePodcastChannel = MutableLiveData<List<PodcastChannel>>(emptyList())

        App.getSubsonicClientInstance(false)
            .podcastClient
            .getPodcasts(includeEpisodes, channelId ?: "")
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.podcasts?.channels?.let {
                            livePodcastChannel.postValue(it)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })

        return livePodcastChannel
    }

    fun getNewestPodcastEpisodes(count: Int): MutableLiveData<List<PodcastEpisode>> {
        val liveNewestPodcastEpisodes = MutableLiveData<List<PodcastEpisode>>(emptyList())

        App.getSubsonicClientInstance(false)
            .podcastClient
            .getNewestPodcasts(count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.newestPodcasts?.episodes?.let {
                            liveNewestPodcastEpisodes.postValue(it)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })

        return liveNewestPodcastEpisodes
    }

    fun refreshPodcasts() {
        App.getSubsonicClientInstance(false)
            .podcastClient
            .refreshPodcasts()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty as per Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })
    }

    fun createPodcastChannel(url: String) {
        App.getSubsonicClientInstance(false)
            .podcastClient
            .createPodcastChannel(url)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty as per Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })
    }

    fun deletePodcastChannel(channelId: String) {
        App.getSubsonicClientInstance(false)
            .podcastClient
            .deletePodcastChannel(channelId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty as per Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })
    }

    fun deletePodcastEpisode(episodeId: String) {
        App.getSubsonicClientInstance(false)
            .podcastClient
            .deletePodcastEpisode(episodeId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty as per Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })
    }

    fun downloadPodcastEpisode(episodeId: String) {
        App.getSubsonicClientInstance(false)
            .podcastClient
            .downloadPodcastEpisode(episodeId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty as per Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty as per Java
                }
            })
    }
}
