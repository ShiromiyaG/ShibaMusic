package com.shirou.shibamusic.github.api.release

import android.util.Log
import com.shirou.shibamusic.github.Github
import com.shirou.shibamusic.github.GithubRetrofitClient
import com.shirou.shibamusic.github.models.LatestRelease
import retrofit2.Call

class ReleaseClient(github: Github) {

    private val releaseService: ReleaseService =
        GithubRetrofitClient(github).retrofit.create(ReleaseService::class.java)

    fun getLatestRelease(): Call<LatestRelease> {
        Log.d(TAG, "getLatestRelease()")
        return releaseService.getLatestRelease(Github.OWNER, Github.REPO)
    }

    companion object {
        private const val TAG = "ReleaseClient"
    }
}
