package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
interface StarCallback {
    fun onError() {}
    fun onSuccess() {}
}
