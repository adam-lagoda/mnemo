package com.lagoda.mnemo.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.lagoda.mnemo.model.ModelId
import com.lagoda.mnemo.model.ModelSpec
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxEmbeddingEngine(private val context: Context) : EmbeddingEngine {

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null
    private var tokenizer: BertTokenizer? = null

    private val modelFile: File
        get() = File(
            context.getExternalFilesDir(null),
            ModelSpec.ALL[ModelId.GTE_SMALL]!!.filename
        )

    val isReady: Boolean
        get() = modelFile.let { it.exists() && it.length() > 0 }

    @Synchronized
    private fun session(): OrtSession? {
        if (session != null) return session
        if (!isReady) return null
        return try {
            env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
                .also { session = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ONNX session", e)
            null
        }
    }

    @Synchronized
    private fun tokenizer(): BertTokenizer? {
        if (tokenizer != null) return tokenizer
        return try {
            BertTokenizer.fromAssets(context).also { tokenizer = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tokenizer vocab — is assets/gte_vocab.txt present?", e)
            null
        }
    }

    override fun embed(text: String): FloatArray {
        val sess = session() ?: return FloatArray(0)
        val tok = tokenizer() ?: return FloatArray(0)
        return try {
            val enc = tok.encode(text)
            val shape = longArrayOf(1L, enc.inputIds.size.toLong())
            val inputIds  = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.inputIds),       shape)
            val attMask   = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.attentionMask),  shape)
            val tokType   = OnnxTensor.createTensor(env, LongBuffer.wrap(enc.tokenTypeIds),   shape)
            val inputs = mapOf(
                "input_ids"      to inputIds,
                "attention_mask" to attMask,
                "token_type_ids" to tokType,
            )
            sess.run(inputs).use { result ->
                var tensor: OnnxTensor? = null
                for ((name, value) in result) {
                    if (name == "last_hidden_state") { tensor = value as OnnxTensor; break }
                }
                val t = tensor ?: return FloatArray(0)
                val seqLen = enc.inputIds.size
                val buf = t.floatBuffer
                val hiddenSize = buf.remaining() / seqLen
                meanPool(buf, seqLen, hiddenSize, enc.attentionMask).also { normalize(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "embed() failed", e)
            FloatArray(0)
        }
    }

    private fun meanPool(
        buf: java.nio.FloatBuffer,
        seqLen: Int,
        hiddenSize: Int,
        mask: LongArray,
    ): FloatArray {
        val out = FloatArray(hiddenSize)
        var count = 0
        for (i in 0 until seqLen) {
            if (i < mask.size && mask[i] == 1L) {
                for (j in 0 until hiddenSize) out[j] += buf.get(i * hiddenSize + j)
                count++
            }
        }
        if (count > 0) for (i in out.indices) out[i] /= count
        return out
    }

    private fun normalize(v: FloatArray) {
        var norm = 0f
        for (x in v) norm += x * x
        norm = sqrt(norm)
        if (norm > 0f) for (i in v.indices) v[i] /= norm
    }

    /** Dot product of two L2-normalised vectors == cosine similarity. */
    override fun similarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot.coerceIn(-1f, 1f)
    }

    fun close() {
        session?.close()
        session = null
    }

    companion object {
        private const val TAG = "OnnxEmbeddingEngine"
    }
}

// ── Serialisation helpers ────────────────────────────────────────────────────

fun FloatArray.toEmbeddingBlob(): ByteArray {
    val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (f in this) buf.putFloat(f)
    return buf.array()
}

fun ByteArray.toFloatVector(): FloatArray {
    val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(size / 4) { buf.getFloat() }
}
