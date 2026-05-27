package com.lagoda.mnemo.data.model

import android.net.Uri

data class ScreenshotCandidate(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val timestamp: Long
)
