package com.mnemo.extraction

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.mnemo.data.model.ExtractionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class GemmaExtractor(private val context: Context) : VlmExtractor {

    private val modelPath: String
        get() = "${context.filesDir.absolutePath}/gemma-3n-E2B-it-int4.litertlm"

    private var engine: Engine? = null
    private var chatConversation: Conversation? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getOrCreateEngine(): Engine {
        return engine ?: run {
            val config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                visionBackend = Backend.GPU(),
                cacheDir = context.cacheDir.path
            )
            Engine(config).also { e ->
                e.initialize()
                engine = e
            }
        }
    }

    override suspend fun extract(bitmap: Bitmap, screenshotUri: String): ExtractionResult? =
        withContext(Dispatchers.IO) {
            val tmpFile = File.createTempFile("screenshot", ".jpg", context.cacheDir)
            try {
                tmpFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                val e = getOrCreateEngine()
                e.createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Contents.of(
                            Content.ImageFile(tmpFile.absolutePath),
                            Content.Text(ExtractionPrompts.buildExtractionPrompt())
                        )
                    ).toString()
                    parseResponse(response)
                }
            } catch (e: Exception) {
                android.util.Log.e("GemmaExtractor", "extract() failed", e)
                null
            } finally {
                tmpFile.delete()
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

    suspend fun generate(prompt: String): String =
        withContext(Dispatchers.IO) {
            val e = getOrCreateEngine()
            e.createConversation().use { conversation ->
                conversation.sendMessage(Contents.of(Content.Text(prompt))).toString()
            }
        }

    fun generateStream(prompt: String): Flow<String> = flow {
        val e = getOrCreateEngine()
        val conversation = if (chatConversation?.isAlive == true) {
            chatConversation!!
        } else {
            e.createConversation().also { chatConversation = it }
        }
        conversation.sendMessageAsync(Contents.of(Content.Text(prompt))).collect { message ->
            emit(message.toString())
        }
    }.flowOn(Dispatchers.IO)

    fun clearChatHistory() {
        chatConversation?.close()
        chatConversation = null
    }

    override fun close() {
        chatConversation?.close()
        chatConversation = null
        engine?.close()
        engine = null
    }
}
