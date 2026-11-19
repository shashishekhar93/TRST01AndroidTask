package com.shekhar.trst01androidtask.ui.edit

import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity

sealed interface EditEmployeeUiState {
    data object Loading : EditEmployeeUiState
    data class Content(
        val employee: EmployeeEntity,
        val isSaving: Boolean = false,
        val isSuccess: Boolean = false,
        val saveError: String? = null
    ) : EditEmployeeUiState
    data object NotFound : EditEmployeeUiState
    data class Error(val message: String) : EditEmployeeUiState
}
