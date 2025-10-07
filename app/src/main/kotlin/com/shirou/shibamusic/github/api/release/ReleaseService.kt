package com.shirou.shibamusic.github.api.release

import com.shirou.shibamusic.github.models.LatestRelease
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ReleaseService {
    @GET("repos/{owner}/{repo}/releases/latest")
    fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<LatestRelease>
}
