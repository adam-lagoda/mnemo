package com.mnemo.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.mnemo.data.model.ScreenshotCandidate

object MediaStoreScanner {
    fun query(
        contentResolver: ContentResolver,
        relativePath: String,
        sinceMillis: Long = 0
    ): List<ScreenshotCandidate> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val selectionParts = mutableListOf(
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        )
        val selectionArgs = mutableListOf("$relativePath/%")

        if (sinceMillis > 0) {
            selectionParts.add("${MediaStore.Images.Media.DATE_TAKEN} >= ?")
            selectionArgs.add(sinceMillis.toString())
        }

        val results = mutableListOf<ScreenshotCandidate>()
        contentResolver.query(
            collection,
            projection,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(collection, id.toString())
                val name = cursor.getString(nameCol) ?: continue
                // DATE_TAKEN can be null/0 on some devices; fall back to DATE_MODIFIED (seconds)
                val timestamp = cursor.getLong(dateTakenCol).takeIf { it > 0 }
                    ?: (cursor.getLong(dateModCol) * 1000L)
                results.add(ScreenshotCandidate(id, uri, name, timestamp))
            }
        }
        return results
    }
}
