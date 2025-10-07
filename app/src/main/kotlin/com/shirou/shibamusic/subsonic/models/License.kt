package com.shirou.shibamusic.subsonic.models

import androidx.annotation.Keep
import java.util.*

@Keep
class License {
    var isValid = false
    var email: String? = null
    var licenseExpires: Date? = null
    var trialExpires: Date? = null
}
