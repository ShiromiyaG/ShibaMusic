package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
interface MediaIndexCallback {
    fun onRecovery(index: Int) {}
}
