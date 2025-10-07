package com.shirou.shibamusic.subsonic.api.medialibraryscanning

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface MediaLibraryScanningService {
    @GET("startScan")
    fun startScan(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getScanStatus")
    fun getScanStatus(@QueryMap params: Map<String, String>): Call<ApiResponse>
}
