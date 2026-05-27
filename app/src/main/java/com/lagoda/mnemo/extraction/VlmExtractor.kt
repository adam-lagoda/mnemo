package com.lagoda.mnemo.extraction

import android.graphics.Bitmap
import com.lagoda.mnemo.data.model.ExtractionResult

interface VlmExtractor {
    suspend fun extract(bitmap: Bitmap, screenshotUri: String): ExtractionResult?
    fun close()
}
