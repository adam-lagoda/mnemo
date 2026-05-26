package com.mnemo.embedding

import android.content.Context

/**
 * STUB — replace with real ONNX Runtime implementation.
 *
 * To implement:
 * 1. Download gte-small or e5-small-v2 INT8 ONNX model
 * 2. Place in assets/models/embedding.onnx
 * 3. Use OrtEnvironment + OrtSession to run inference
 * 4. Tokenize text (BPE or WordPiece) before passing to model
 *
 * Interface contract: embed() returns a 384-dim float vector (gte-small output dim).
 */
class OnnxEmbeddingEngine(private val context: Context) : EmbeddingEngine {

    override fun embed(text: String): FloatArray {
        // TODO: implement ONNX inference
        // val env = OrtEnvironment.getEnvironment()
        // val session = env.createSession(modelBytes, OrtSession.SessionOptions())
        // ...
        throw UnsupportedOperationException("OnnxEmbeddingEngine not yet implemented — use TfIdfFallbackEngine")
    }

    override fun similarity(a: FloatArray, b: FloatArray): Float {
        throw UnsupportedOperationException("OnnxEmbeddingEngine not yet implemented")
    }
}
