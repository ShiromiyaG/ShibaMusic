package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
interface DialogClickCallback {
    fun onPositiveClick() {}
    fun onNegativeClick() {}
    fun onNeutralClick() {}
}
