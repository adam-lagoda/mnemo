package com.mnemo.data.prefs

import android.content.Context
import android.net.Uri

class AppConfig(context: Context) {
    private val prefs = context.getSharedPreferences("mnemo_config", Context.MODE_PRIVATE)

    var treeUri: Uri?
        get() = prefs.getString("tree_uri", null)?.let { Uri.parse(it) }
        set(value) = prefs.edit().putString("tree_uri", value?.toString()).apply()

    var dayFilter: Int
        get() = prefs.getInt("day_filter", 30)
        set(value) = prefs.edit().putInt("day_filter", value).apply()

    var hfToken: String?
        get() = prefs.getString("hf_token", null)
        set(value) = prefs.edit().putString("hf_token", value).apply()

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
        val DAY_FILTER_OPTIONS = listOf(7, 30, 90, -1)
        fun dayFilterLabel(days: Int) = if (days == -1) "All" else "${days}d"
    }
}
