package com.shekhar.trst01androidtask.ui.add

sealed interface AddEmployeeUiState {
    data object Idle : AddEmployeeUiState
    data object Loading : AddEmployeeUiState
    data object Success : AddEmployeeUiState
    data class Error(val message: String) : AddEmployeeUiState
}
