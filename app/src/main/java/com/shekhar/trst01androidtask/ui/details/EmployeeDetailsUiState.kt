package com.shekhar.trst01androidtask.ui.details

import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity

sealed interface EmployeeDetailsUiState {
    data object Loading : EmployeeDetailsUiState
    data class Success(
        val employee: EmployeeEntity,
        val documents: List<DocumentEntity>
    ) : EmployeeDetailsUiState
    data object NotFound : EmployeeDetailsUiState
    data class Error(val message: String) : EmployeeDetailsUiState
}
