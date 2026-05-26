package com.mnemo.data.prefs

import android.content.Context
import android.net.Uri

class AppConfig(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var treeUri: Uri?
        get() = prefs.getString(KEY_TREE_URI, null)?.let { Uri.parse(it) }
        set(value) = prefs.edit().putString(KEY_TREE_URI, value?.toString()).apply()

    var dayFilter: Int
        get() = prefs.getInt(KEY_DAY_FILTER, 30)
        set(value) = prefs.edit().putInt(KEY_DAY_FILTER, value).apply()

    var hfToken: String?
        get() = prefs.getString(KEY_HF_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_HF_TOKEN, value).apply()

    /** Last path segment of the selected folder, e.g. "Screenshots" */
    val displayName: String
        get() = treeUri?.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?: ""

    /**
     * Relative path extracted from the SAF tree URI for MediaStore queries.
     * e.g. "primary:Pictures/Screenshots" → "Pictures/Screenshots"
     */
    val relativePath: String?
        get() = treeUri?.lastPathSegment
            ?.substringAfter(":", "")
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val PREFS_NAME    = "mnemo_config"
        private const val KEY_TREE_URI  = "tree_uri"
        private const val KEY_DAY_FILTER = "day_filter"
        private const val KEY_HF_TOKEN  = "hf_token"

        val DAY_FILTER_OPTIONS = listOf(7, 30, 90, -1)
        fun dayFilterLabel(days: Int) = if (days == -1) "All" else "${days}d"
    }
}
