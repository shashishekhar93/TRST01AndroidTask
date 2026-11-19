package com.shekhar.trst01androidtask.ui

import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private lateinit var viewModel: EmployeeViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val employeesFlow = MutableStateFlow<List<EmployeeEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeEmployees() } returns employeesFlow
        coEvery { repository.refreshEmployees() } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadingAndEmptyState() = runTest {
        viewModel = EmployeeViewModel(repository)
        assertTrue(viewModel.uiState.value is EmployeeUiState.Loading)
    }

    @Test
    fun populatedRoomEmissionsSuccessState() = runTest {
        val employee = EmployeeEntity(
            employeeId = 1,
            employeeName = "Alice Smith",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-01-01",
            updatedDate = null
        )
        employeesFlow.value = listOf(employee)
        viewModel = EmployeeViewModel(repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeUiState.Success)
        assertEquals(1, (state as EmployeeUiState.Success).employees.size)
        assertEquals("Alice Smith", state.employees[0].employeeName)
    }

    @Test
    fun successfulRefresh() = runTest {
        viewModel = EmployeeViewModel(repository)
        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.refreshEmployees() }
    }

    @Test
    fun failedRefreshPreservesLocalData() = runTest {
        val employee = EmployeeEntity(
            employeeId = 2,
            employeeName = "Bob Jones",
            departmentId = 2,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-01-01",
            updatedDate = null
        )
        employeesFlow.value = listOf(employee)
        coEvery { repository.refreshEmployees() } returns Result.Error(IOException("Network error"))

        viewModel = EmployeeViewModel(repository)
        viewModel.refresh()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeUiState.Success)
        assertEquals(1, (state as EmployeeUiState.Success).employees.size)
        assertEquals("Bob Jones", state.employees[0].employeeName)
    }

    @Test
    fun emptyRoomAfterSuccessfulSync() = runTest {
        employeesFlow.value = emptyList()
        coEvery { repository.refreshEmployees() } returns Result.Success(Unit)

        viewModel = EmployeeViewModel(repository)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is EmployeeUiState.Empty)
    }

    @Test
    fun emptyRoomAfterApiFailure() = runTest {
        employeesFlow.value = emptyList()
        coEvery { repository.refreshEmployees() } returns Result.Error(IOException("Server error"))

        viewModel = EmployeeViewModel(repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeUiState.Error)
    }

    @Test
    fun searchFilterBehavior() = runTest {
        val emp1 = EmployeeEntity(1, "Alice Smith", 10, null, true, 1, "2023-01-01", null)
        val emp2 = EmployeeEntity(2, "Bob Jones", 20, null, true, 1, "2023-01-01", null)
        employeesFlow.value = listOf(emp1, emp2)

        viewModel = EmployeeViewModel(repository)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.employees.value.size)

        viewModel.setSearchQuery("Alice")
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.employees.value.size)
        assertEquals("Alice Smith", viewModel.employees.value[0].employeeName)
    }

    @Test
    fun noDuplicateInitialRefresh() = runTest {
        viewModel = EmployeeViewModel(repository)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.refreshEmployees() }
    }
}
