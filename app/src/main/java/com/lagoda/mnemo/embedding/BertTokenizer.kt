package com.lagoda.mnemo.embedding

import android.content.Context

/**
 * BERT WordPiece tokenizer for GTE-small (bert-base-uncased vocabulary).
 *
 * Requires: app/src/main/assets/gte_vocab.txt
 * Download from: https://huggingface.co/thenlper/gte-small/resolve/main/vocab.txt
 * Place at: app/src/main/assets/gte_vocab.txt
 */
class BertTokenizer(private val vocab: Map<String, Int>) {

    data class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
    )

    fun encode(text: String, maxTokens: Int = 512): Encoding {
        val words = basicTokenize(text.lowercase())
        val wpIds = mutableListOf<Long>()
        for (word in words) {
            val pieces = wordPiece(word)
            if (wpIds.size + pieces.size > maxTokens - 2) break  // reserve for [CLS] and [SEP]
            wpIds.addAll(pieces)
        }

        val len = wpIds.size + 2  // +[CLS] +[SEP]
        val inputIds = LongArray(len)
        val mask = LongArray(len) { 1L }

        inputIds[0] = CLS_ID
        wpIds.forEachIndexed { i, id -> inputIds[i + 1] = id }
        inputIds[len - 1] = SEP_ID

        return Encoding(
            inputIds = inputIds,
            attentionMask = mask,
            tokenTypeIds = LongArray(len) { 0L },
        )
    }

    private fun basicTokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val buf = StringBuilder()
        for (ch in text) {
            when {
                ch.isWhitespace() -> flush(buf, tokens)
                ch.isLetterOrDigit() || ch == '\'' -> buf.append(ch)
                else -> {
                    flush(buf, tokens)
                    tokens.add(ch.toString())
                }
            }
        }
        flush(buf, tokens)
        return tokens
    }

    private fun flush(buf: StringBuilder, out: MutableList<String>) {
        if (buf.isNotEmpty()) { out.add(buf.toString()); buf.clear() }
    }

    private fun wordPiece(word: String): List<Long> {
        val ids = mutableListOf<Long>()
        var remaining = word
        var isFirst = true
        while (remaining.isNotEmpty()) {
            var matched = false
            for (end in remaining.length downTo 1) {
                val sub = if (isFirst) remaining.substring(0, end)
                          else "##${remaining.substring(0, end)}"
                val id = vocab[sub]
                if (id != null) {
                    ids.add(id.toLong())
                    remaining = remaining.substring(end)
                    isFirst = false
                    matched = true
                    break
                }
            }
            if (!matched) {
                ids.add(UNK_ID)
                break
            }
        }
        return ids
    }

    companion object {
        const val PAD_ID = 0L
        const val UNK_ID = 100L
        const val CLS_ID = 101L
        const val SEP_ID = 102L

        fun fromAssets(context: Context): BertTokenizer {
            val vocab = context.assets.open("gte_vocab.txt").bufferedReader().useLines { lines ->
                buildMap { lines.forEachIndexed { idx, line -> put(line.trim(), idx) } }
            }
            return BertTokenizer(vocab)
        }
    }
}
