package com.mnemo.data.repository

import com.mnemo.data.db.ScreenshotDao
import com.mnemo.data.db.entities.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

class ScreenshotRepository(private val dao: ScreenshotDao) {
    fun observeAll(): Flow<List<ScreenshotEntity>> = dao.observeAll()
    fun observeByCommunity(communityId: Int): Flow<List<ScreenshotEntity>> =
        dao.observeByCommunity(communityId)
    fun observeUnextractedCount(): Flow<Int> = dao.observeUnextractedCount()
    suspend fun getAll(): List<ScreenshotEntity> = dao.getAll()
    suspend fun getUnextracted(): List<ScreenshotEntity> = dao.getUnextracted()
    suspend fun getUnreviewedSince(since: Long): List<ScreenshotEntity> =
        dao.getUnreviewedSince(since)
    suspend fun insert(screenshot: ScreenshotEntity) = dao.insert(screenshot)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>) = dao.insertAll(screenshots)
    suspend fun update(screenshot: ScreenshotEntity) = dao.update(screenshot)
    suspend fun updateCommunity(id: String, communityId: Int) = dao.updateCommunity(id, communityId)
    suspend fun markReviewed(id: String) = dao.markReviewed(id)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    suspend fun search(query: String): List<ScreenshotEntity> = dao.search(query)
    suspend fun getById(id: String): ScreenshotEntity? = dao.getById(id)
    suspend fun getAllUris(): List<String> = dao.getAllUris()
}
