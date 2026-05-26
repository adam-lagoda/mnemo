package com.mnemo.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mnemo.data.db.entities.ScreenshotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotDaoTest {
    private lateinit var db: MnemoDatabase
    private lateinit var dao: ScreenshotDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MnemoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.screenshotDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun observeUnextractedCount_countsNullExtractedJsonOnly() = runBlocking {
        dao.insert(ScreenshotEntity(id = "1", uri = "u1", timestamp = 0L, extractedJson = null))
        dao.insert(ScreenshotEntity(id = "2", uri = "u2", timestamp = 0L, extractedJson = "{}"))
        dao.insert(ScreenshotEntity(id = "3", uri = "u3", timestamp = 0L, extractedJson = null))

        val count = dao.observeUnextractedCount().first()

        assertEquals(2, count)
    }

    @Test
    fun observeUnextractedCount_emitsZeroWhenAllExtracted() = runBlocking {
        dao.insert(ScreenshotEntity(id = "1", uri = "u1", timestamp = 0L, extractedJson = "{}"))

        val count = dao.observeUnextractedCount().first()

        assertEquals(0, count)
    }

    @Test
    fun existsByUri_returnsTrueWhenUriPresent() = runBlocking {
        dao.insert(ScreenshotEntity(id = "id-1", uri = "content://media/external/images/media/1", timestamp = 0L))

        val result = dao.existsByUri("content://media/external/images/media/1")

        assertTrue(result)
    }

    @Test
    fun existsByUri_returnsFalseWhenUriAbsent() = runBlocking {
        val result = dao.existsByUri("content://media/external/images/media/99")

        assertFalse(result)
    }

    @Test
    fun existsByUri_matchesExactUriOnly() = runBlocking {
        dao.insert(ScreenshotEntity(id = "id-1", uri = "content://media/external/images/media/1", timestamp = 0L))

        val result = dao.existsByUri("content://media/external/images/media/10")

        assertFalse(result)
    }
}
