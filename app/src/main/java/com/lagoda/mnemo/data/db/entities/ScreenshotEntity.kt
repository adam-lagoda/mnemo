package com.lagoda.mnemo.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val timestamp: Long,
    val sourceType: String = "other",
    val extractedJson: String? = null,
    val embeddingBlob: ByteArray? = null,
    val communityId: Int = -1,
    val reviewed: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenshotEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}
