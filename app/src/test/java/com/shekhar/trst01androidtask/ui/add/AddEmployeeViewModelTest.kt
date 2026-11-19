package com.shekhar.trst01androidtask.ui.add

import androidx.lifecycle.SavedStateHandle
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import com.shekhar.trst01androidtask.data.repository.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddEmployeeViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddEmployeeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.generateNextEmployeeId() } returns 1001
        coEvery { repository.insertEmployee(any()) } returns Result.Success(Unit)
        viewModel = AddEmployeeViewModel(SavedStateHandle(), repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiStateIsIdle() {
        assertTrue(viewModel.uiState.value is AddEmployeeUiState.Idle)
        assertTrue(viewModel.validationState.value.isValid)
    }

    @Test
    fun saveEmployee_missingName_validationFails() = runTest {
        viewModel.saveEmployee(
            name = "",
            departmentIdStr = "1",
            branchIdStr = "1",
            managerIdStr = "",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        val validation = viewModel.validationState.value
        assertFalse(validation.isValid)
        assertEquals("Employee name is required", validation.nameError)
        assertTrue(viewModel.uiState.value is AddEmployeeUiState.Idle)
    }

    @Test
    fun saveEmployee_invalidNumericFields_validationFails() = runTest {
        viewModel.saveEmployee(
            name = "John Doe",
            departmentIdStr = "-5",
            branchIdStr = "abc",
            managerIdStr = "-10",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        val validation = viewModel.validationState.value
        assertFalse(validation.isValid)
        assertEquals("Department ID must be a positive number", validation.departmentError)
        assertEquals("Branch ID must be a positive number", validation.branchError)
        assertEquals("Manager ID must be a positive number", validation.managerError)
    }

    @Test
    fun saveEmployee_validData_generatesIdAndPersists() = runTest {
        viewModel.saveEmployee(
            name = "  Alice Smith  ",
            departmentIdStr = "5",
            branchIdStr = "2",
            managerIdStr = "101",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AddEmployeeUiState.Success)

        val captured = mutableListOf<EmployeeEntity>()
        coVerify { repository.insertEmployee(capture(captured)) }

        assertEquals(1, captured.size)
        val saved = captured[0]
        assertEquals(1001, saved.employeeId)
        assertEquals("Alice Smith", saved.employeeName)
        assertEquals(5, saved.departmentId)
        assertEquals(2, saved.branchId)
        assertEquals(101, saved.managerId)
        assertTrue(saved.isActive == true)
        assertNotNull(saved.createdDate)
        assertNotNull(saved.updatedDate)
        assertEquals(saved.createdDate, saved.updatedDate)
    }

    @Test
    fun saveEmployee_repositoryError_emitsErrorState() = runTest {
        coEvery { repository.insertEmployee(any()) } returns Result.Error(IOException("DB write failed"))

        viewModel.saveEmployee(
            name = "Bob",
            departmentIdStr = "1",
            branchIdStr = "1",
            managerIdStr = "",
            isActive = false
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddEmployeeUiState.Error)
        assertEquals("DB write failed", (state as AddEmployeeUiState.Error).message)
    }

    @Test
    fun saveEmployee_duplicateSubmissionBlocked() = runTest {
        viewModel.saveEmployee(
            name = "Carol",
            departmentIdStr = "1",
            branchIdStr = "1",
            managerIdStr = "",
            isActive = true
        )
        // Immediately attempt duplicate call before coroutine finishes
        viewModel.saveEmployee(
            name = "Carol",
            departmentIdStr = "1",
            branchIdStr = "1",
            managerIdStr = "",
            isActive = true
        )
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.insertEmployee(any()) }
    }
}
