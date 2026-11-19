package com.shekhar.trst01androidtask.ui.delete

import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity

sealed interface DeleteEmployeeUiState {
    data object Loading : DeleteEmployeeUiState
    data class Content(
        val employee: EmployeeEntity,
        val isDeleting: Boolean = false,
        val isSuccess: Boolean = false,
        val deleteError: String? = null
    ) : DeleteEmployeeUiState
    data object NotFound : DeleteEmployeeUiState
}
