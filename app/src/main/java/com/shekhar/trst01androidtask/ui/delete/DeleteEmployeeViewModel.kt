package com.shekhar.trst01androidtask.ui.delete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import com.shekhar.trst01androidtask.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteEmployeeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1

    private val _isDeleting = MutableStateFlow(false)
    private val _isSuccess = MutableStateFlow(false)
    private val _deleteError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DeleteEmployeeUiState> = if (employeeId == -1) {
        flowOf(DeleteEmployeeUiState.NotFound)
    } else {
        combine(
            repository.observeEmployeeById(employeeId),
            _isDeleting,
            _isSuccess,
            _deleteError
        ) { employee, isDeleting, isSuccess, deleteError ->
            if (employee == null && !isSuccess) {
                DeleteEmployeeUiState.NotFound
            } else if (employee != null) {
                DeleteEmployeeUiState.Content(
                    employee = employee,
                    isDeleting = isDeleting,
                    isSuccess = isSuccess,
                    deleteError = deleteError
                )
            } else {
                // Employee was deleted successfully (employee is now null in DB)
                DeleteEmployeeUiState.Content(
                    employee = EmployeeEntity(
                        employeeId = employeeId,
                        employeeName = "",
                        departmentId = null,
                        managerId = null,
                        isActive = false,
                        branchId = null,
                        createdDate = null,
                        updatedDate = null
                    ),
                    isDeleting = false,
                    isSuccess = true,
                    deleteError = null
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DeleteEmployeeUiState.Loading
    )

    fun confirmDelete() {
        val currentState = uiState.value
        if (currentState !is DeleteEmployeeUiState.Content || currentState.isDeleting || currentState.isSuccess) {
            return
        }

        _isDeleting.value = true
        _deleteError.value = null
        viewModelScope.launch {
            try {
                val result = repository.deleteEmployeeById(employeeId)
                when (result) {
                    is Result.Success -> {
                        _isDeleting.value = false
                        _isSuccess.value = true
                    }
                    is Result.Error -> {
                        _isDeleting.value = false
                        _deleteError.value = result.message ?: "Failed to delete employee from database"
                    }
                }
            } catch (e: Exception) {
                _isDeleting.value = false
                _deleteError.value = e.message ?: "An unexpected error occurred during deletion"
            }
        }
    }

    fun resetError() {
        _deleteError.value = null
    }
}
