package com.mnemo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractionResult(
    val source_type: String = "other",
    val title: String = "",
    val entities: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val action_items: List<String> = emptyList(),
    val summary: String = "",
    val sentiment: String = "neutral",
    val urgency: Float = 0f,
    val language: String = "en"
)
