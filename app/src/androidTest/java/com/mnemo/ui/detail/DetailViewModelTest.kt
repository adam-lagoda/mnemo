package com.mnemo.ui.detail

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mnemo.data.db.MnemoDatabase
import com.mnemo.data.db.entities.ScreenshotEntity
import com.mnemo.data.repository.GraphRepository
import com.mnemo.data.repository.ScreenshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: MnemoDatabase
    private lateinit var screenshotRepo: ScreenshotRepository
    private lateinit var graphRepo: GraphRepository
    private lateinit var vm: DetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MnemoDatabase::class.java
        ).allowMainThreadQueries().build()
        screenshotRepo = ScreenshotRepository(db.screenshotDao())
        graphRepo = GraphRepository(db.graphEdgeDao())
        val app = ApplicationProvider.getApplicationContext<Application>()
        vm = DetailViewModel(app, screenshotRepo, graphRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun delete_removesScreenshotFromDb() = runTest {
        screenshotRepo.insert(ScreenshotEntity(id = "s1", uri = "u1", timestamp = 0L))
        vm.load("s1")
        val job = launch { vm.uiState.collect {} }
        var deleteCalled = false

        vm.delete { deleteCalled = true }

        assertTrue(deleteCalled)
        assertNull(screenshotRepo.getById("s1"))
        job.cancel()
    }
}
