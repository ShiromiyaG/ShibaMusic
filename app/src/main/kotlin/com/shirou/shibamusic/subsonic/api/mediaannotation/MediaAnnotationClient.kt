package com.shirou.shibamusic.subsonic.api.mediaannotation

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class MediaAnnotationClient(private val subsonic: Subsonic) {

    private val mediaAnnotationService: MediaAnnotationService =
        RetrofitClient(subsonic).retrofit.create(MediaAnnotationService::class.java)

    fun star(id: String?, albumId: String?, artistId: String?): Call<ApiResponse> {
        Log.d(TAG, "star()")
        val call = when {
            !id.isNullOrBlank() -> mediaAnnotationService.starSong(subsonic.params, id)
            !albumId.isNullOrBlank() -> mediaAnnotationService.starAlbum(subsonic.params, albumId)
            !artistId.isNullOrBlank() -> mediaAnnotationService.starArtist(subsonic.params, artistId)
            else -> throw IllegalArgumentException("At least one identifier (id, albumId or artistId) must be provided to star media")
        }
        return call
    }

    fun unstar(id: String?, albumId: String?, artistId: String?): Call<ApiResponse> {
        Log.d(TAG, "unstar()")
        val call = when {
            !id.isNullOrBlank() -> mediaAnnotationService.unstarSong(subsonic.params, id)
            !albumId.isNullOrBlank() -> mediaAnnotationService.unstarAlbum(subsonic.params, albumId)
            !artistId.isNullOrBlank() -> mediaAnnotationService.unstarArtist(subsonic.params, artistId)
            else -> throw IllegalArgumentException("At least one identifier (id, albumId or artistId) must be provided to unstar media")
        }
        return call
    }

    fun setRating(id: String, rating: Int): Call<ApiResponse> {
        Log.d(TAG, "setRating()")
        return mediaAnnotationService.setRating(subsonic.params, id, rating)
    }

    fun scrobble(id: String, submission: Boolean?): Call<ApiResponse> {
        Log.d(TAG, "scrobble()")
        return mediaAnnotationService.scrobble(subsonic.params, id, submission)
    }

    companion object {
        private const val TAG = "MediaAnnotationClient"
    }
}
