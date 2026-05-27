package com.lagoda.mnemo.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapUtils {
    private const val MAX_DIMENSION = 768

    suspend fun loadAndResize(contentResolver: ContentResolver, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // Decode bounds first to calculate sample size
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, MAX_DIMENSION)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext null

                // Scale down if still too large
                val maxDim = maxOf(bitmap.width, bitmap.height)
                if (maxDim <= MAX_DIMENSION) return@withContext bitmap
                val scale = MAX_DIMENSION.toFloat() / maxDim
                val matrix = Matrix().apply { setScale(scale, scale) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    .also { if (it !== bitmap) bitmap.recycle() }
            } catch (e: Exception) {
                null
            }
        }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var size = 1
        val largerDim = maxOf(width, height)
        while (largerDim / (size * 2) >= maxDim) size *= 2
        return size
    }
}
