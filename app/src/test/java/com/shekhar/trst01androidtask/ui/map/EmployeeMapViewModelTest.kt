package com.shekhar.trst01androidtask.ui.map

import androidx.lifecycle.SavedStateHandle
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import com.shekhar.trst01androidtask.data.repository.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeMapViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val mapAreaFlow = MutableStateFlow<MapAreaEntity?>(null)
    private val testEmployee = EmployeeEntity(
        employeeId = 101,
        employeeName = "John Doe",
        departmentId = 1,
        managerId = null,
        isActive = true,
        branchId = 1,
        createdDate = "2023-01-01",
        updatedDate = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getEmployeeById(101) } returns testEmployee
        every { repository.observeMapAreaForEmployee(101) } returns mapAreaFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingEmployeeIdReturnsNotFound() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        assertEquals(EmployeeMapUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun nonExistentEmployeeReturnsNotFound() = runTest {
        coEvery { repository.getEmployeeById(999) } returns null
        every { repository.observeMapAreaForEmployee(999) } returns MutableStateFlow(null)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 999))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        assertEquals(EmployeeMapUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun loadingEmployeeWithNoSavedPolygonInitializesEmptyMap() = runTest {
        mapAreaFlow.value = null
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeMapUiState.Success)
        val successState = state as EmployeeMapUiState.Success
        assertEquals(101, successState.employeeId)
        assertEquals("John Doe", successState.employeeName)
        assertTrue(successState.points.isEmpty())
        assertFalse(successState.isSavedInDb)
        assertFalse(successState.hasUnsavedChanges)
    }

    @Test
    fun loadingEmployeeWithSavedPolygonRestoresCoordinates() = runTest {
        val savedPoints = listOf(
            LatLngPoint(12.9716, 77.5946),
            LatLngPoint(13.0827, 80.2707),
            LatLngPoint(12.9141, 74.8560)
        )
        mapAreaFlow.value = MapAreaEntity(employeeId = 101, coordinates = savedPoints)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertEquals(3, state.points.size)
        assertTrue(state.isSavedInDb)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(savedPoints, state.points)
    }

    @Test
    fun addPointAppendsCoordinateAndMarksUnsavedChanges() = runTest {
        mapAreaFlow.value = null
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val point1 = LatLngPoint(10.0, 20.0)
        viewModel.addPoint(point1)

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertEquals(1, state.points.size)
        assertEquals(point1, state.points[0])
        assertEquals(0, state.selectedIndex)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun movePointUpdatesCoordinateAtIndex() = runTest {
        val initialPoints = listOf(
            LatLngPoint(10.0, 10.0),
            LatLngPoint(20.0, 20.0),
            LatLngPoint(30.0, 30.0)
        )
        mapAreaFlow.value = MapAreaEntity(employeeId = 101, coordinates = initialPoints)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val movedPoint = LatLngPoint(25.0, 25.0)
        viewModel.movePoint(1, movedPoint)

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertEquals(movedPoint, state.points[1])
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun removeSelectedPointRemovesVertex() = runTest {
        val initialPoints = listOf(
            LatLngPoint(10.0, 10.0),
            LatLngPoint(20.0, 20.0),
            LatLngPoint(30.0, 30.0)
        )
        mapAreaFlow.value = MapAreaEntity(employeeId = 101, coordinates = initialPoints)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.selectPoint(1)
        viewModel.removeSelectedPoint()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertEquals(2, state.points.size)
        assertNull(state.selectedIndex)
        assertEquals(LatLngPoint(30.0, 30.0), state.points[1])
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun saveMapArea_lessThanThreePoints_showsValidationError() = runTest {
        mapAreaFlow.value = null
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.addPoint(LatLngPoint(10.0, 10.0))
        viewModel.addPoint(LatLngPoint(20.0, 20.0))

        viewModel.saveMapArea()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertEquals("A polygon requires at least 3 points.", state.validationError)
        coVerify(exactly = 0) { repository.saveMapArea(any(), any()) }
    }

    @Test
    fun saveMapArea_validPolygon_savesSuccessfully() = runTest {
        mapAreaFlow.value = null
        coEvery { repository.saveMapArea(101, any()) } returns Result.Success(Unit)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.addPoint(LatLngPoint(10.0, 10.0))
        viewModel.addPoint(LatLngPoint(20.0, 20.0))
        viewModel.addPoint(LatLngPoint(30.0, 30.0))

        viewModel.saveMapArea()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertTrue(state.isSavedInDb)
        assertFalse(state.hasUnsavedChanges)
        assertEquals("Map area saved successfully.", state.userMessage)
        coVerify { repository.saveMapArea(101, state.points) }
    }

    @Test
    fun saveMapArea_repositoryFailure_emitsErrorMessage() = runTest {
        mapAreaFlow.value = null
        coEvery { repository.saveMapArea(101, any()) } returns Result.Error(IOException("Database save failed"))

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.addPoint(LatLngPoint(10.0, 10.0))
        viewModel.addPoint(LatLngPoint(20.0, 20.0))
        viewModel.addPoint(LatLngPoint(30.0, 30.0))

        viewModel.saveMapArea()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertFalse(state.isSaving)
        assertEquals("Database save failed", state.errorMessage)
    }

    @Test
    fun clearMapArea_removesPolygonAndResetsState() = runTest {
        val initialPoints = listOf(
            LatLngPoint(10.0, 10.0),
            LatLngPoint(20.0, 20.0),
            LatLngPoint(30.0, 30.0)
        )
        mapAreaFlow.value = MapAreaEntity(employeeId = 101, coordinates = initialPoints)
        coEvery { repository.clearMapArea(101) } returns Result.Success(Unit)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.clearMapArea()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertTrue(state.points.isEmpty())
        assertFalse(state.isSavedInDb)
        assertFalse(state.hasUnsavedChanges)
        assertEquals("Map area cleared successfully.", state.userMessage)
        coVerify { repository.clearMapArea(101) }
    }

    @Test
    fun clearMapArea_repositoryFailure_emitsErrorMessage() = runTest {
        val initialPoints = listOf(
            LatLngPoint(10.0, 10.0),
            LatLngPoint(20.0, 20.0),
            LatLngPoint(30.0, 30.0)
        )
        mapAreaFlow.value = MapAreaEntity(employeeId = 101, coordinates = initialPoints)
        coEvery { repository.clearMapArea(101) } returns Result.Error(IOException("Clear failed"))

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.clearMapArea()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertFalse(state.isClearing)
        assertEquals("Clear failed", state.errorMessage)
    }

    @Test
    fun preventingInvalidCoordinatesFromBeingSaved() = runTest {
        mapAreaFlow.value = null
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EmployeeMapViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.addPoint(LatLngPoint(95.0, 10.0)) // Invalid lat > 90

        val state = viewModel.uiState.value as EmployeeMapUiState.Success
        assertTrue(state.points.isEmpty())
        assertEquals("Invalid latitude or longitude coordinate.", state.errorMessage)
    }
}
