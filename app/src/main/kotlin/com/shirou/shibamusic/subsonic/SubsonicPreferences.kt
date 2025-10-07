package com.shirou.shibamusic.subsonic

import com.shirou.shibamusic.subsonic.utils.StringUtil
import java.util.UUID

class SubsonicPreferences {
    var serverUrl: String? = null
    var username: String? = null
    var clientName: String = "ShibaMusic"
    var authentication: SubsonicAuthentication? = null

    fun setAuthentication(password: String?, token: String?, salt: String?, isLowSecurity: Boolean) {
        if (!password.isNullOrEmpty()) {
            authentication = SubsonicAuthentication(password, isLowSecurity)
            return
        }

        if (!token.isNullOrEmpty() && !salt.isNullOrEmpty()) {
            authentication = SubsonicAuthentication(token, salt)
        } else {
            authentication = null
        }
    }

    class SubsonicAuthentication {
        var password: String? = null
            private set
        var salt: String? = null
            private set
        var token: String? = null
            private set

        constructor(password: String, isLowSecurity: Boolean) {
            if (isLowSecurity) {
                this.password = password
            } else {
                update(password)
            }
        }

        constructor(token: String, salt: String) {
            this.token = token
            this.salt = salt
        }

        fun update(password: String) {
            salt = UUID.randomUUID().toString()
            token = StringUtil.tokenize(password + salt)
        }
    }
}
