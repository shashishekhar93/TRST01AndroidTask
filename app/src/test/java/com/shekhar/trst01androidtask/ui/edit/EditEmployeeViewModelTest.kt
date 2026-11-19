package com.shekhar.trst01androidtask.ui.edit

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EditEmployeeViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val employeeFlow = MutableStateFlow<EmployeeEntity?>(null)

    private val existingEmployee = EmployeeEntity(
        employeeId = 101,
        employeeName = "David Miller",
        departmentId = 3,
        managerId = 1,
        isActive = true,
        branchId = 2,
        createdDate = "2023-01-01T10:00:00Z",
        updatedDate = "2023-01-01T10:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeEmployeeById(101) } returns employeeFlow
        coEvery { repository.insertEmployee(any()) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingEmployeeIdReturnsNotFound() = runTest {
        val viewModel = EditEmployeeViewModel(SavedStateHandle(), repository)
        testScheduler.advanceUntilIdle()

        assertEquals(EditEmployeeUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun loadsExistingEmployeeSuccessfully() = runTest {
        employeeFlow.value = existingEmployee
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EditEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditEmployeeUiState.Content)
        val content = state as EditEmployeeUiState.Content
        assertEquals("David Miller", content.employee.employeeName)
        assertEquals(3, content.employee.departmentId)
    }

    @Test
    fun updateEmployee_validData_preservesCreatedDateAndUpdatesUpdatedDate() = runTest {
        employeeFlow.value = existingEmployee
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EditEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.updateEmployee(
            name = "David Miller Updated",
            departmentIdStr = "4",
            branchIdStr = "3",
            managerIdStr = "2",
            isActive = false
        )
        testScheduler.advanceUntilIdle()

        val captured = mutableListOf<EmployeeEntity>()
        coVerify { repository.insertEmployee(capture(captured)) }

        assertEquals(1, captured.size)
        val updated = captured[0]
        assertEquals(101, updated.employeeId)
        assertEquals("David Miller Updated", updated.employeeName)
        assertEquals(4, updated.departmentId)
        assertEquals(3, updated.branchId)
        assertEquals(2, updated.managerId)
        assertFalse(updated.isActive == true)
        assertEquals("2023-01-01T10:00:00Z", updated.createdDate) // Preserved!
        assertNotEquals("2023-01-01T10:00:00Z", updated.updatedDate) // Updated timestamp!
    }

    @Test
    fun updateEmployee_invalidData_failsValidation() = runTest {
        employeeFlow.value = existingEmployee
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EditEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.updateEmployee(
            name = "",
            departmentIdStr = "invalid",
            branchIdStr = "",
            managerIdStr = "-1",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        val validation = viewModel.validationState.value
        assertFalse(validation.isValid)
        assertEquals("Employee name is required", validation.nameError)
        assertEquals("Department ID must be a positive number", validation.departmentError)
        assertEquals("Branch ID is required", validation.branchError)

        coVerify(exactly = 0) { repository.insertEmployee(any()) }
    }

    @Test
    fun updateEmployee_repositoryError_emitsErrorState() = runTest {
        employeeFlow.value = existingEmployee
        coEvery { repository.insertEmployee(any()) } returns Result.Error(IOException("Update error"))
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        val viewModel = EditEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.updateEmployee(
            name = "David",
            departmentIdStr = "1",
            branchIdStr = "1",
            managerIdStr = "",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditEmployeeUiState.Content)
        val content = state as EditEmployeeUiState.Content
        assertEquals("Update error", content.saveError)
        assertFalse(content.isSaving)
        assertFalse(content.isSuccess)
    }
}
