package com.mnemo.model

enum class ModelId { GEMMA, GTE_SMALL }

sealed class ModelDownloadState {
    object Absent : ModelDownloadState()
    data class Downloading(val progress: Float, val downloadId: Long) : ModelDownloadState()
    object Validating : ModelDownloadState()   // file move after download
    object Ready : ModelDownloadState()         // file present and non-empty
    object Verifying : ModelDownloadState()     // loading model into memory to confirm it works
    object Verified : ModelDownloadState()      // model successfully loaded into memory
    data class Failed(val reason: String) : ModelDownloadState()
}

data class ModelSpec(
    val id: ModelId,
    val name: String,
    val filename: String,
    val url: String,
    val requiresAuth: Boolean = false
) {
    companion object {
        val ALL: Map<ModelId, ModelSpec> = mapOf(
            ModelId.GEMMA to ModelSpec(
                id = ModelId.GEMMA,
                name = "Gemma 3n E2B",
                filename = "gemma-3n-E2B-it-int4.litertlm",
                url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
                requiresAuth = false
            ),
            ModelId.GTE_SMALL to ModelSpec(
                id = ModelId.GTE_SMALL,
                name = "GTE Small (embedding)",
                filename = "model_qint8_avx512_vnni.onnx",
                url = "https://huggingface.co/thenlper/gte-small/resolve/main/onnx/model_qint8_avx512_vnni.onnx",
                requiresAuth = false
            )
        )
    }
}
