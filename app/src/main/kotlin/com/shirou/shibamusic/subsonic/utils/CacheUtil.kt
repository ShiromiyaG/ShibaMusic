package com.shirou.shibamusic.subsonic.utils

import com.shirou.shibamusic.util.NetworkUtil
import okhttp3.Interceptor

class CacheUtil(private val maxAge: Int, private val maxStale: Int) {

    val onlineInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAge")
            .removeHeader("Pragma")
            .build()
    }

    val offlineInterceptor = Interceptor { chain ->
        var request = chain.request()
        if (!NetworkUtil.isConnected()) {
            request = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .removeHeader("Pragma")
                .build()
        }
        chain.proceed(request)
    }
}
