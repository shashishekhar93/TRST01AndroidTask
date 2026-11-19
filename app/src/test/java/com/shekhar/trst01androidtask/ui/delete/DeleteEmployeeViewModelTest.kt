package com.shekhar.trst01androidtask.ui.delete

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteEmployeeViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val employeeFlow = MutableStateFlow<EmployeeEntity?>(null)

    private val sampleEmployee = EmployeeEntity(
        employeeId = 202,
        employeeName = "Sarah Connor",
        departmentId = 2,
        managerId = null,
        isActive = true,
        branchId = 1,
        createdDate = "2023-01-01",
        updatedDate = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeEmployeeById(202) } returns employeeFlow
        coEvery { repository.deleteEmployeeById(202) } returns Result.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingEmployeeIdReturnsNotFound() = runTest {
        val viewModel = DeleteEmployeeViewModel(SavedStateHandle(), repository)
        testScheduler.advanceUntilIdle()

        assertEquals(DeleteEmployeeUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun loadsEmployeeForDeletionConfirmation() = runTest {
        employeeFlow.value = sampleEmployee
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 202))
        val viewModel = DeleteEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DeleteEmployeeUiState.Content)
        val content = state as DeleteEmployeeUiState.Content
        assertEquals("Sarah Connor", content.employee.employeeName)
        assertFalse(content.isDeleting)
        assertFalse(content.isSuccess)
    }

    @Test
    fun confirmDelete_successRemovesEmployee() = runTest {
        employeeFlow.value = sampleEmployee
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 202))
        val viewModel = DeleteEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.confirmDelete()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteEmployeeById(202) }

        val state = viewModel.uiState.value
        assertTrue(state is DeleteEmployeeUiState.Content)
        val content = state as DeleteEmployeeUiState.Content
        assertTrue(content.isSuccess)
        assertFalse(content.isDeleting)
    }

    @Test
    fun confirmDelete_failureEmitsErrorState() = runTest {
        employeeFlow.value = sampleEmployee
        coEvery { repository.deleteEmployeeById(202) } returns Result.Error(IOException("Delete constraint failed"))

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 202))
        val viewModel = DeleteEmployeeViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        viewModel.confirmDelete()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DeleteEmployeeUiState.Content)
        val content = state as DeleteEmployeeUiState.Content
        assertFalse(content.isSuccess)
        assertFalse(content.isDeleting)
        assertEquals("Delete constraint failed", content.deleteError)
    }
}
