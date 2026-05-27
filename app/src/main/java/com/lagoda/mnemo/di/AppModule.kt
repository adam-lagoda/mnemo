package com.lagoda.mnemo.di

import android.content.Context
import com.lagoda.mnemo.data.db.MnemoDatabase
import com.lagoda.mnemo.data.repository.GraphRepository
import com.lagoda.mnemo.data.repository.ScreenshotRepository
import com.lagoda.mnemo.embedding.OnnxEmbeddingEngine
import com.lagoda.mnemo.embedding.TfIdfFallbackEngine
import com.lagoda.mnemo.extraction.GemmaExtractor
import com.lagoda.mnemo.extraction.VlmExtractor
import com.lagoda.mnemo.data.prefs.AppConfig
import com.lagoda.mnemo.graph.GraphAnalytics
import com.lagoda.mnemo.graph.GraphBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppModule private constructor(context: Context) {
    private val db = MnemoDatabase.getInstance(context)
    val screenshotRepository = ScreenshotRepository(db.screenshotDao())
    val graphRepository = GraphRepository(db.graphEdgeDao())

    val embeddingEngine = TfIdfFallbackEngine()
    val onnxEmbeddingEngine = OnnxEmbeddingEngine(context)
    val vlmExtractor: VlmExtractor = GemmaExtractor(context)
    val graphBuilder = GraphBuilder(embeddingEngine, screenshotRepository)
    val graphAnalytics = GraphAnalytics()
    val appConfig = AppConfig(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedEmbeddingCorpus() {
        scope.launch {
            val texts = screenshotRepository.getAll()
                .mapNotNull { it.extractedJson }
            embeddingEngine.rebuildCorpus(texts)
        }
    }

    companion object {
        @Volatile private var INSTANCE: AppModule? = null

        fun getInstance(context: Context): AppModule {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppModule(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
