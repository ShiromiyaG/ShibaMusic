package com.shirou.shibamusic.util

import com.shirou.shibamusic.App

/**
 * Extra preference keys kept separate to avoid merge conflicts.
 */
object PreferencesExt {
    private const val LAST_INDEXES_MODIFIED = "last_indexes_modified"

    fun getLastIndexesModified(): Long {
        return App.getInstance().preferences.getLong(LAST_INDEXES_MODIFIED, 0L)
    }

    fun setLastIndexesModified(value: Long) {
        App.getInstance().preferences.edit().putLong(LAST_INDEXES_MODIFIED, value).apply()
    }
}