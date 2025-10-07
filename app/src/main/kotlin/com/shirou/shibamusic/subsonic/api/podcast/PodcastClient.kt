package com.shirou.shibamusic.subsonic.api.podcast

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class PodcastClient(private val subsonic: Subsonic) {

    private val podcastService: PodcastService =
        RetrofitClient(subsonic).retrofit.create(PodcastService::class.java)

    fun getPodcasts(includeEpisodes: Boolean, channelId: String?): Call<ApiResponse> {
        Log.d(TAG, "getPodcasts()")
        return podcastService.getPodcasts(subsonic.params, includeEpisodes, channelId)
    }

    fun getNewestPodcasts(count: Int): Call<ApiResponse> {
        Log.d(TAG, "getNewestPodcasts()")
        return podcastService.getNewestPodcasts(subsonic.params, count)
    }

    fun refreshPodcasts(): Call<ApiResponse> {
        Log.d(TAG, "refreshPodcasts()")
        return podcastService.refreshPodcasts(subsonic.params)
    }

    fun createPodcastChannel(url: String): Call<ApiResponse> {
        Log.d(TAG, "createPodcastChannel()")
        return podcastService.createPodcastChannel(subsonic.params, url)
    }

    fun deletePodcastChannel(channelId: String): Call<ApiResponse> {
        Log.d(TAG, "deletePodcastChannel()")
        return podcastService.deletePodcastChannel(subsonic.params, channelId)
    }

    fun deletePodcastEpisode(episodeId: String): Call<ApiResponse> {
        Log.d(TAG, "deletePodcastEpisode()")
        return podcastService.deletePodcastEpisode(subsonic.params, episodeId)
    }

    fun downloadPodcastEpisode(episodeId: String): Call<ApiResponse> {
        Log.d(TAG, "downloadPodcastEpisode()")
        return podcastService.downloadPodcastEpisode(subsonic.params, episodeId)
    }

    companion object {
        private const val TAG = "PodcastClient"
    }
}
