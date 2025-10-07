package com.shirou.shibamusic.repository

import com.shirou.shibamusic.App
import com.shirou.shibamusic.interfaces.ScanCallback
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanRepository {

    private fun handleScanResponse(response: Response<ApiResponse>, callback: ScanCallback) {
        if (response.isSuccessful) {
            val subsonicResponse = response.body()?.subsonicResponse ?: return
            val error = subsonicResponse.error
            if (error != null) {
                callback.onError(Exception(error.message ?: "Scan error"))
                return
            }

            val scanStatus = subsonicResponse.scanStatus ?: return
            val count = scanStatus.count ?: 0L
            callback.onSuccess(scanStatus.isScanning, count)
        }
    }

    private fun createScanApiCallback(callback: ScanCallback): Callback<ApiResponse> {
        return object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                handleScanResponse(response, callback)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                callback.onError(Exception(t.message))
            }
        }
    }

    fun startScan(callback: ScanCallback) {
        App.getSubsonicClientInstance(false)
            .mediaLibraryScanningClient
            .startScan()
            .enqueue(createScanApiCallback(callback))
    }

    fun getScanStatus(callback: ScanCallback) {
        App.getSubsonicClientInstance(false)
            .mediaLibraryScanningClient
            .getScanStatus()
            .enqueue(createScanApiCallback(callback))
    }
}
