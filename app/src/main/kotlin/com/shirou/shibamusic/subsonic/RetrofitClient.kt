package com.shirou.shibamusic.subsonic

import com.shirou.shibamusic.App
import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.subsonic.utils.CacheUtil
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class RetrofitClient(subsonic: Subsonic) {
    var retrofit: Retrofit

    init {
        retrofit = Retrofit.Builder()
            .baseUrl(subsonic.url)
            .client(sharedOkHttp)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()))
            .build()
    }

    companion object {
        private val cacheUtil = CacheUtil(60 * 60, 7 * 24 * 60 * 60)

        private fun httpLogging(): HttpLoggingInterceptor {
            val logging = HttpLoggingInterceptor()
            logging.level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
            return logging
        }

        private fun httpCache(): Cache {
            val cacheDir = File(App.getContext().cacheDir, "http_cache")
            return Cache(cacheDir, 50L * 1024L * 1024L) // 50MB
        }

        val sharedOkHttp: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Accept", "application/json")
                            .build()
                    )
                }
                .addInterceptor(httpLogging())
                .addInterceptor(cacheUtil.offlineInterceptor)
                .addNetworkInterceptor(cacheUtil.onlineInterceptor)
                .cache(httpCache())
                .build()
        }
    }
}
