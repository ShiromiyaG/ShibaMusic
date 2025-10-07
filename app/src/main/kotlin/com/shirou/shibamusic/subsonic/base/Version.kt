package com.shirou.shibamusic.subsonic.base

class Version private constructor(val versionString: String) : Comparable<Version> {

    companion object {
        private val VERSION_REGEX = Regex("\\d+(\\.\\d+)*")

        fun of(versionString: String?): Version {
            require(versionString != null && VERSION_REGEX.matches(versionString)) {
                "Invalid version format"
            }
            return Version(versionString)
        }
    }

    fun isLowerThan(other: Version): Boolean = compareTo(other) < 0

    override fun compareTo(other: Version): Int {
        val thisParts = versionString.split('.')
        val otherParts = other.versionString.split('.')
        val length = maxOf(thisParts.size, otherParts.size)

        for (index in 0 until length) {
            val thisPart = thisParts.getOrNull(index)?.toInt() ?: 0
            val otherPart = otherParts.getOrNull(index)?.toInt() ?: 0

            if (thisPart != otherPart) {
                return thisPart.compareTo(otherPart)
            }
        }
        return 0
    }

    override fun toString(): String = versionString
}
