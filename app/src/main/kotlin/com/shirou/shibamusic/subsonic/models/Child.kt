package com.shirou.shibamusic.subsonic.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
@Parcelize
open class Child(
    open val id: String,
    @SerializedName("parent")
    var parentId: String? = null,
    var isDir: Boolean = false,
    var title: String? = null,
    var album: String? = null,
    var artist: String? = null,
    var track: Int? = null,
    var year: Int? = null,
    @SerializedName("genre")
    var genre: String? = null,
    @SerializedName("coverArt")
    var coverArtId: String? = null,
    var size: Long? = null,
    var contentType: String? = null,
    var suffix: String? = null,
    @SerializedName("transcoding_content_type")
    var transcodedContentType: String? = null,
    var transcodedSuffix: String? = null,
    var duration: Int? = null,
    @SerializedName("bitRate")
    var bitrate: Int? = null,
    @SerializedName("samplingRate")
    var samplingRate: Int? = null,
    @SerializedName("bitDepth")
    var bitDepth: Int? = null,
    var path: String? = null,
    @SerializedName("isVideo")
    var isVideo: Boolean = false,
    var userRating: Int? = null,
    var averageRating: Double? = null,
    @SerializedName("playCount")
    var playCount: Long? = null,
    var discNumber: Int? = null,
    var created: Date? = null,
    var starred: Date? = null,
    var albumId: String? = null,
    var artistId: String? = null,
    var type: String? = null,
    var bookmarkPosition: Long? = null,
    var originalWidth: Int? = null,
    var originalHeight: Int? = null
) : Parcelable
