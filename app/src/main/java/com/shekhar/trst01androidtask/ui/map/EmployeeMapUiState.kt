package com.shekhar.trst01androidtask.ui.map

import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint

sealed interface EmployeeMapUiState {
    data object Loading : EmployeeMapUiState
    data object NotFound : EmployeeMapUiState

    data class Success(
        val employeeId: Int,
        val employeeName: String?,
        val points: List<LatLngPoint> = emptyList(),
        val selectedIndex: Int? = null,
        val isSavedInDb: Boolean = false,
        val isSaving: Boolean = false,
        val isClearing: Boolean = false,
        val validationError: String? = null,
        val userMessage: String? = null,
        val errorMessage: String? = null,
        val hasUnsavedChanges: Boolean = false
    ) : EmployeeMapUiState
}
