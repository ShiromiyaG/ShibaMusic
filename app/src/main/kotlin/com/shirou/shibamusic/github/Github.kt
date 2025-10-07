package com.shirou.shibamusic.github

import com.shirou.shibamusic.github.api.release.ReleaseClient

class Github {

    val releaseClient: ReleaseClient by lazy { ReleaseClient(this) }

    val url: String = "https://api.github.com/"

    companion object {
        const val OWNER = "ShiromiyaG"
        const val REPO = "ShibaMusic"
    }
}
