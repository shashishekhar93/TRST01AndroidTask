package com.shekhar.trst01androidtask.ui.preview

import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity

sealed interface DocumentPreviewUiState {
    data object Loading : DocumentPreviewUiState
    data class Success(val document: DocumentEntity) : DocumentPreviewUiState
    data object NotFound : DocumentPreviewUiState
    data class Error(val message: String) : DocumentPreviewUiState
}
