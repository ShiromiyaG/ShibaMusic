package com.shirou.shibamusic.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

object Util {

    fun <T> distinctByKey(keyExtractor: (T) -> Any?): Predicate<T>? {
        return runCatching {
            val sentinel = Any()
            val uniqueMap = ConcurrentHashMap<Any, Boolean>()
            Predicate<T> { item ->
                val key = keyExtractor(item) ?: sentinel
                uniqueMap.putIfAbsent(key, true) == null
            }
        }.getOrNull()
    }

    fun toPascalCase(name: String?): String? {
        if (name.isNullOrEmpty()) {
            return name
        }

        val result = StringBuilder()
        var toUpper = false

        name.forEachIndexed { index, char ->
            when {
                index == 0 -> result.append(char.uppercaseChar())
                char == '_' -> toUpper = true
                toUpper -> {
                    result.append(char.uppercaseChar())
                    toUpper = false
                }
                else -> result.append(char)
            }
        }

        return result.toString()
    }

    fun encode(value: String): String {
        return runCatching {
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)
    }
}
