package com.shirou.shibamusic.subsonic.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object StringUtil {
    private const val MD5 = "MD5"

    fun tokenize(value: String): String {
        return try {
            val digest = MessageDigest.getInstance(MD5)
            val messageDigest = digest.digest(value.toByteArray())
            messageDigest.joinToString(separator = "") { byte ->
                byte.toInt().and(0xFF).toString(16).padStart(2, '0')
            }
        } catch (exception: NoSuchAlgorithmException) {
            exception.printStackTrace()
            ""
        }
    }
}
