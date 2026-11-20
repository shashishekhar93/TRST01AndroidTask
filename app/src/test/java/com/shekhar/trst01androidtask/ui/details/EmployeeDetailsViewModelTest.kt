package com.shekhar.trst01androidtask.ui.details

import androidx.lifecycle.SavedStateHandle
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeDetailsViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val employeeFlow = MutableStateFlow<EmployeeEntity?>(null)
    private val documentsFlow = MutableStateFlow<List<DocumentEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeMapAreaForEmployee(any()) } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingEmployeeIdReturnsNotFound() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = EmployeeDetailsViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        assertEquals(EmployeeDetailsUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun employeeNotFoundInRoomReturnsNotFoundState() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 999))
        every { repository.observeEmployeeById(999) } returns employeeFlow
        every { repository.observeDocumentsForEmployee(999) } returns documentsFlow

        val viewModel = EmployeeDetailsViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        assertEquals(EmployeeDetailsUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun validEmployeeWithNoDocumentsReturnsSuccessState() = runTest {
        val emp = EmployeeEntity(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 5,
            managerId = 1,
            isActive = true,
            branchId = 2,
            createdDate = "2023-01-01",
            updatedDate = "2023-06-01"
        )
        employeeFlow.value = emp
        documentsFlow.value = emptyList()

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101))
        every { repository.observeEmployeeById(101) } returns employeeFlow
        every { repository.observeDocumentsForEmployee(101) } returns documentsFlow

        val viewModel = EmployeeDetailsViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeDetailsUiState.Success)
        val successState = state as EmployeeDetailsUiState.Success
        assertEquals("John Doe", successState.employee.employeeName)
        assertTrue(successState.documents.isEmpty())
    }

    @Test
    fun validEmployeeWithMultipleDocumentsReturnsSuccessState() = runTest {
        val emp = EmployeeEntity(
            employeeId = 102,
            employeeName = "Jane Smith",
            departmentId = 3,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-02-01",
            updatedDate = null
        )
        val doc1 = DocumentEntity(
            documentId = 1,
            employeeId = 102,
            documentName = "Passport",
            documentType = "ID",
            isVerified = true,
            createdDate = "2023-02-01",
            updatedDate = null,
            documentUrl = "http://example.com/passport.jpg",
            thumbnailUrl = "http://example.com/thumb.jpg"
        )
        val doc2 = DocumentEntity(
            documentId = 2,
            employeeId = 102,
            documentName = "Driver License",
            documentType = "ID",
            isVerified = false,
            createdDate = "2023-02-01",
            updatedDate = null,
            documentUrl = "http://example.com/license.jpg",
            thumbnailUrl = null
        )

        employeeFlow.value = emp
        documentsFlow.value = listOf(doc1, doc2)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 102))
        every { repository.observeEmployeeById(102) } returns employeeFlow
        every { repository.observeDocumentsForEmployee(102) } returns documentsFlow

        val viewModel = EmployeeDetailsViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EmployeeDetailsUiState.Success)
        val successState = state as EmployeeDetailsUiState.Success
        assertEquals(2, successState.documents.size)
        assertEquals("Passport", successState.documents[0].documentName)
        assertEquals("Driver License", successState.documents[1].documentName)
    }

    @Test
    fun documentFlowEmissionsUpdateUiStateDynamically() = runTest {
        val emp = EmployeeEntity(103, "Dynamic User", 1, null, true, 1, "2023-01-01", null)
        employeeFlow.value = emp
        documentsFlow.value = emptyList()

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 103))
        every { repository.observeEmployeeById(103) } returns employeeFlow
        every { repository.observeDocumentsForEmployee(103) } returns documentsFlow

        val viewModel = EmployeeDetailsViewModel(savedStateHandle, repository)
        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as EmployeeDetailsUiState.Success
        assertEquals(0, state.documents.size)

        // Emit a new document
        val newDoc = DocumentEntity(10, 103, "Aadhaar", "ID", true, "2023-01-01", null, "url", "thumb")
        documentsFlow.value = listOf(newDoc)
        testScheduler.advanceUntilIdle()

        state = viewModel.uiState.value as EmployeeDetailsUiState.Success
        assertEquals(1, state.documents.size)
        assertEquals("Aadhaar", state.documents[0].documentName)
    }
}
