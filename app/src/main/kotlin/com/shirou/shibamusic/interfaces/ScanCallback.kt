package com.shirou.shibamusic.interfaces

interface ScanCallback {
    fun onError(exception: Exception) {}
    fun onSuccess(isScanning: Boolean, count: Long) {}
}
