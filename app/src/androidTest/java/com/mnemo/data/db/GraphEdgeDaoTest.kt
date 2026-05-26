package com.mnemo.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mnemo.data.db.entities.GraphEdgeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphEdgeDaoTest {
    private lateinit var db: MnemoDatabase
    private lateinit var dao: GraphEdgeDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MnemoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.graphEdgeDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun deleteEdgesForNode_removesEdgesWhereNodeIsSourceOrTarget() = runBlocking {
        dao.insertAll(listOf(
            GraphEdgeEntity("a", "b", 0.5f, "semantic"),
            GraphEdgeEntity("b", "c", 0.7f, "semantic"),
            GraphEdgeEntity("c", "d", 0.3f, "semantic")
        ))

        dao.deleteEdgesForNode("b")

        val remaining = dao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(GraphEdgeEntity("c", "d", 0.3f, "semantic"), remaining.first())
    }

    @Test
    fun deleteEdgesForNode_leavesUnrelatedEdgesUntouched() = runBlocking {
        dao.insertAll(listOf(
            GraphEdgeEntity("x", "y", 1.0f, "temporal"),
            GraphEdgeEntity("y", "z", 0.9f, "temporal")
        ))

        dao.deleteEdgesForNode("a")

        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun deleteEdgesForNode_onNodeWithNoEdges_doesNothing() = runBlocking {
        dao.insertAll(listOf(GraphEdgeEntity("p", "q", 0.5f, "entity")))

        dao.deleteEdgesForNode("z")

        assertEquals(1, dao.getAll().size)
    }
}
