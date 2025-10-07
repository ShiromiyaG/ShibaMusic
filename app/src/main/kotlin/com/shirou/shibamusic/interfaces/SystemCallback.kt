package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
interface SystemCallback {
    fun onError(exception: Exception) {}
    fun onSuccess(password: String, token: String, salt: String) {}
}
