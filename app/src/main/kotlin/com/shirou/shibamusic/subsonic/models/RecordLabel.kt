package com.shirou.shibamusic.subsonic.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
open class RecordLabel : Parcelable {
    var name: String? = null
}
