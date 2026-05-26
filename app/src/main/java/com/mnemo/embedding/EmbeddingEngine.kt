package com.mnemo.embedding

interface EmbeddingEngine {
    /** Returns a float vector representing the text. Empty array if engine not ready. */
    fun embed(text: String): FloatArray

    /** Cosine similarity in [0, 1]. Returns 0 if either vector is empty or mismatched. */
    fun similarity(a: FloatArray, b: FloatArray): Float
}
