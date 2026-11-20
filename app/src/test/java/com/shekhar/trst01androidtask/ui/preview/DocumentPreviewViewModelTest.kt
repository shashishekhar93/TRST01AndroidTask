package com.shekhar.trst01androidtask.ui.preview

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentPreviewViewModelTest {

    private val repository: EmployeeRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val documentsFlow = MutableStateFlow<List<DocumentEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingArgumentsReturnsNotFoundState() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = DocumentPreviewViewModel(savedStateHandle, repository, context, okHttpClient)
        testScheduler.advanceUntilIdle()

        assertEquals(DocumentPreviewUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun validArgumentsFindsMatchingDocumentAndReturnsSuccessState() = runTest {
        val doc1 = DocumentEntity(1, 101, "Passport", "ID", true, "2023-01-01", null, "url1", "thumb1")
        val doc2 = DocumentEntity(2, 101, "Aadhaar", "ID", true, "2023-01-01", null, "url2", "thumb2")
        documentsFlow.value = listOf(doc1, doc2)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101, "documentId" to 2))
        every { repository.observeDocumentsForEmployee(101) } returns documentsFlow

        val viewModel = DocumentPreviewViewModel(savedStateHandle, repository, context, okHttpClient)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DocumentPreviewUiState.Success)
        val successState = state as DocumentPreviewUiState.Success
        assertEquals("Aadhaar", successState.document.documentName)
    }

    @Test
    fun documentNotFoundInEmittedListReturnsNotFoundState() = runTest {
        val doc1 = DocumentEntity(1, 101, "Passport", "ID", true, "2023-01-01", null, "url1", "thumb1")
        documentsFlow.value = listOf(doc1)

        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101, "documentId" to 999))
        every { repository.observeDocumentsForEmployee(101) } returns documentsFlow

        val viewModel = DocumentPreviewViewModel(savedStateHandle, repository, context, okHttpClient)
        testScheduler.advanceUntilIdle()

        assertEquals(DocumentPreviewUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun repositoryErrorEmitsErrorState() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("employeeId" to 101, "documentId" to 1))
        every { repository.observeDocumentsForEmployee(101) } returns flow {
            throw RuntimeException("Database error")
        }

        val viewModel = DocumentPreviewViewModel(savedStateHandle, repository, context, okHttpClient)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DocumentPreviewUiState.Error)
        assertEquals("Database error", (state as DocumentPreviewUiState.Error).message)
    }
}
