package com.mnemo.embedding

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * TF-IDF based embedding engine. Acts as fallback until ONNX model is wired.
 * Must call addDocument() for all existing texts before using embed() for similarity.
 */
class TfIdfFallbackEngine : EmbeddingEngine {

    private val vocabulary = mutableMapOf<String, Int>()
    private val docFrequency = mutableMapOf<Int, Int>()
    private var docCount = 0

    @Synchronized
    fun addDocument(text: String) {
        docCount++
        tokenize(text).toSet().forEach { word ->
            val idx = vocabulary.getOrPut(word) { vocabulary.size }
            docFrequency[idx] = (docFrequency[idx] ?: 0) + 1
        }
    }

    @Synchronized
    fun rebuildCorpus(texts: List<String>) {
        vocabulary.clear()
        docFrequency.clear()
        docCount = 0
        texts.forEach { addDocument(it) }
    }

    override fun embed(text: String): FloatArray {
        val vocabSize = vocabulary.size
        if (vocabSize == 0) return FloatArray(0)

        val tf = mutableMapOf<Int, Float>()
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return FloatArray(vocabSize)

        tokens.forEach { word ->
            val idx = vocabulary[word] ?: return@forEach
            tf[idx] = (tf[idx] ?: 0f) + 1f
        }
        val maxTf = tf.values.maxOrNull() ?: 1f

        val vector = FloatArray(vocabSize)
        tf.forEach { (idx, freq) ->
            val idf = ln((docCount + 1f) / ((docFrequency[idx] ?: 0) + 1f)) + 1f
            vector[idx] = (freq / maxTf) * idf
        }
        return vector
    }

    override fun similarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }
}
