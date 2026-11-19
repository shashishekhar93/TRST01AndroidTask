package com.shekhar.trst01androidtask.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DocumentPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1
    val documentId: Int = savedStateHandle.get<Int>("documentId") ?: -1

    val uiState: StateFlow<DocumentPreviewUiState> = if (employeeId == -1 || documentId == -1) {
        flowOf(DocumentPreviewUiState.NotFound)
    } else {
        repository.observeDocumentsForEmployee(employeeId)
            .map { docs ->
                val document = docs.find { it.documentId == documentId }
                if (document != null) {
                    DocumentPreviewUiState.Success(document)
                } else {
                    DocumentPreviewUiState.NotFound
                }
            }
            .catch { e ->
                emit(DocumentPreviewUiState.Error(e.message ?: "Failed to load document"))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DocumentPreviewUiState.Loading
    )
}
