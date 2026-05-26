package com.mnemo.extraction

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.mnemo.data.model.ExtractionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * VLM extractor using Gemma 3n E2B via MediaPipe LLM Inference API.
 *
 * Model setup: Copy gemma-3n-E2B-it-int4.bin to the device:
 *   adb push gemma-3n-E2B-it-int4.bin /data/local/tmp/
 * Then grant the app access by placing it at context.filesDir:
 *   adb shell cp /data/local/tmp/gemma-3n-E2B-it-int4.bin \
 *     /data/data/com.mnemo/files/gemma-3n-E2B-it-int4.bin
 *
 * Model download: https://www.kaggle.com/models/google/gemma-3n/tfLite
 */
class GemmaExtractor(private val context: Context) : VlmExtractor {

    private val modelPath: String
        get() = "${context.filesDir.absolutePath}/gemma-3n-E2B-it-int4.bin"

    private var llmInference: LlmInference? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getOrCreateInference(): LlmInference {
        return llmInference ?: run {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.1f)
                .build()
            LlmInference.createFromOptions(context, options).also { llmInference = it }
        }
    }

    override suspend fun extract(bitmap: Bitmap, screenshotUri: String): ExtractionResult? =
        withContext(Dispatchers.IO) {
            try {
                val inference = getOrCreateInference()
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .build()
                val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
                session.addQueryChunk(ExtractionPrompts.buildExtractionPrompt())
                val mpImage = BitmapImageBuilder(bitmap).build()
                session.addImage(mpImage)
                val response = session.generateResponse()
                session.close()
                parseResponse(response)
            } catch (e: Exception) {
                null
            }
        }

    private fun parseResponse(response: String): ExtractionResult? {
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd <= jsonStart) return null
        return try {
            json.decodeFromString<ExtractionResult>(response.substring(jsonStart, jsonEnd + 1))
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        llmInference?.close()
        llmInference = null
    }
}
