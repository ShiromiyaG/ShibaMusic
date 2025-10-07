package com.shirou.shibamusic.subsonic.base

import androidx.annotation.Keep
import com.shirou.shibamusic.subsonic.models.SubsonicResponse
import com.google.gson.annotations.SerializedName

@Keep
class ApiResponse {
    @SerializedName("subsonic-response")
    lateinit var subsonicResponse: SubsonicResponse
}
