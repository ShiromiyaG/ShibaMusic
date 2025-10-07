package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.interfaces.ScanCallback
import com.shirou.shibamusic.repository.ScanRepository

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingViewModel"
    }

    private val scanRepository: ScanRepository = ScanRepository()

    fun launchScan(callback: ScanCallback) {
        scanRepository.startScan(object : ScanCallback {
            override fun onError(exception: Exception) {
                callback.onError(exception)
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                callback.onSuccess(isScanning, count)
            }
        })
    }

    fun getScanStatus(callback: ScanCallback) {
        scanRepository.getScanStatus(object : ScanCallback {
            override fun onError(exception: Exception) {
                callback.onError(exception)
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                callback.onSuccess(isScanning, count)
            }
        })
    }
}
