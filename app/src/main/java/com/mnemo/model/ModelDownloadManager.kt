package com.mnemo.model

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.getSystemService

data class DownloadQueryResult(
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val reason: Int  // HTTP status code on failure (401, 403…) or DownloadManager.ERROR_* constant
)

object ModelDownloadManager {

    fun enqueue(context: Context, spec: ModelSpec, hfToken: String? = null): Long {
        val manager = context.getSystemService<DownloadManager>()!!
        val request = DownloadManager.Request(Uri.parse(spec.url)).apply {
            setTitle(spec.name)
            setDescription("Downloading model…")
            setDestinationInExternalFilesDir(context, null, spec.filename)
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            if (hfToken != null) {
                addRequestHeader("Authorization", "Bearer $hfToken")
            }
        }
        return manager.enqueue(request)
    }

    fun cancel(context: Context, downloadId: Long) {
        val manager = context.getSystemService<DownloadManager>()!!
        manager.remove(downloadId)
    }

    fun queryStatus(context: Context, downloadId: Long): DownloadQueryResult? {
        val manager = context.getSystemService<DownloadManager>()!!
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytesDownloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val totalBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return DownloadQueryResult(status, bytesDownloaded, totalBytes, reason)
        }
        return null
    }
}
