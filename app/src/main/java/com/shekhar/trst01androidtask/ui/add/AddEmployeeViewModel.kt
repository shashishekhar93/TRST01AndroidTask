package com.shekhar.trst01androidtask.ui.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import com.shekhar.trst01androidtask.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AddEmployeeFormValidation(
    val nameError: String? = null,
    val departmentError: String? = null,
    val branchError: String? = null,
    val managerError: String? = null,
    val isValid: Boolean = true
)

@HiltViewModel
class AddEmployeeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEmployeeUiState>(AddEmployeeUiState.Idle)
    val uiState: StateFlow<AddEmployeeUiState> = _uiState.asStateFlow()

    private val _validationState = MutableStateFlow(AddEmployeeFormValidation())
    val validationState: StateFlow<AddEmployeeFormValidation> = _validationState.asStateFlow()

    fun saveEmployee(
        name: String,
        departmentIdStr: String,
        branchIdStr: String,
        managerIdStr: String,
        isActive: Boolean
    ) {
        if (_uiState.value is AddEmployeeUiState.Loading || _uiState.value is AddEmployeeUiState.Success) {
            return
        }

        val trimmedName = name.trim()
        val nameErr = if (trimmedName.isBlank()) "Employee name is required" else null

        val deptId = departmentIdStr.trim().toIntOrNull()
        val deptErr = when {
            departmentIdStr.trim().isBlank() -> "Department ID is required"
            deptId == null || deptId <= 0 -> "Department ID must be a positive number"
            else -> null
        }

        val branchId = branchIdStr.trim().toIntOrNull()
        val branchErr = when {
            branchIdStr.trim().isBlank() -> "Branch ID is required"
            branchId == null || branchId <= 0 -> "Branch ID must be a positive number"
            else -> null
        }

        val managerId = if (managerIdStr.trim().isBlank()) {
            null
        } else {
            managerIdStr.trim().toIntOrNull()
        }
        val managerErr = if (managerIdStr.trim().isNotBlank() && (managerId == null || managerId <= 0)) {
            "Manager ID must be a positive number"
        } else {
            null
        }

        val isValid = nameErr == null && deptErr == null && branchErr == null && managerErr == null
        _validationState.value = AddEmployeeFormValidation(
            nameError = nameErr,
            departmentError = deptErr,
            branchError = branchErr,
            managerError = managerErr,
            isValid = isValid
        )

        if (!isValid) return

        _uiState.value = AddEmployeeUiState.Loading
        viewModelScope.launch {
            try {
                val newId = repository.generateNextEmployeeId()
                val nowIso = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val employee = EmployeeEntity(
                    employeeId = newId,
                    employeeName = trimmedName,
                    departmentId = deptId,
                    managerId = managerId,
                    isActive = isActive,
                    branchId = branchId,
                    createdDate = nowIso,
                    updatedDate = nowIso
                )

                val result = repository.insertEmployee(employee)
                when (result) {
                    is Result.Success -> {
                        _uiState.value = AddEmployeeUiState.Success
                    }
                    is Result.Error -> {
                        _uiState.value = AddEmployeeUiState.Error(
                            result.message ?: "Failed to save employee to database"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddEmployeeUiState.Error(
                    e.message ?: "An unexpected error occurred while saving employee"
                )
            }
        }
    }

    fun resetError() {
        if (_uiState.value is AddEmployeeUiState.Error) {
            _uiState.value = AddEmployeeUiState.Idle
        }
    }
}
