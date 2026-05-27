package com.lagoda.mnemo.ui.gallery

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class GalleryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: GalleryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        vm = GalleryViewModel(app)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun enterSelectionMode_setsSelectionModeAndAddsId() = runTest {
        val job = launch { vm.uiState.collect {} }

        vm.enterSelectionMode("shot1")

        val state = vm.uiState.value
        assertTrue(state.isSelectionMode)
        assertEquals(setOf("shot1"), state.selectedIds)
        job.cancel()
    }

    @Test
    fun toggleSelection_removesAlreadySelectedId() = runTest {
        val job = launch { vm.uiState.collect {} }
        vm.enterSelectionMode("shot1")

        vm.toggleSelection("shot1")

        assertFalse("shot1" in vm.uiState.value.selectedIds)
        job.cancel()
    }

    @Test
    fun toggleSelection_addsUnselectedId() = runTest {
        val job = launch { vm.uiState.collect {} }
        vm.enterSelectionMode("shot1")

        vm.toggleSelection("shot2")

        assertEquals(setOf("shot1", "shot2"), vm.uiState.value.selectedIds)
        job.cancel()
    }

    @Test
    fun exitSelectionMode_clearsSelectionState() = runTest {
        val job = launch { vm.uiState.collect {} }
        vm.enterSelectionMode("shot1")

        vm.exitSelectionMode()

        val state = vm.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedIds.isEmpty())
        job.cancel()
    }
}
