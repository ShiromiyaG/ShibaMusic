package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
fun interface DecadesCallback {
    fun onLoadYear(year: Int)
}
