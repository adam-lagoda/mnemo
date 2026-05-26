package com.mnemo.data.db

import androidx.room.*
import com.mnemo.data.db.entities.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenshot: ScreenshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>)

    @Update
    suspend fun update(screenshot: ScreenshotEntity)

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    suspend fun getAll(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE extractedJson IS NULL ORDER BY timestamp DESC")
    suspend fun getUnextracted(): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM screenshots WHERE extractedJson IS NULL")
    fun observeUnextractedCount(): Flow<Int>

    @Query("SELECT * FROM screenshots WHERE reviewed = 0 AND timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getUnreviewedSince(since: Long): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE communityId = :communityId ORDER BY timestamp DESC")
    fun observeByCommunity(communityId: Int): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE id = :id")
    suspend fun getById(id: String): ScreenshotEntity?

    @Query("UPDATE screenshots SET communityId = :communityId WHERE id = :id")
    suspend fun updateCommunity(id: String, communityId: Int)

    @Query("UPDATE screenshots SET reviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: String)

    @Query("SELECT * FROM screenshots WHERE extractedJson LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun search(query: String): List<ScreenshotEntity>

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT uri FROM screenshots")
    suspend fun getAllUris(): List<String>
}
