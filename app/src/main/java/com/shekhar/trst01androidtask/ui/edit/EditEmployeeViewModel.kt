package com.shekhar.trst01androidtask.ui.edit

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditEmployeeFormValidation(
    val nameError: String? = null,
    val departmentError: String? = null,
    val branchError: String? = null,
    val managerError: String? = null,
    val isValid: Boolean = true
)

@HiltViewModel
class EditEmployeeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1

    private val _isSaving = MutableStateFlow(false)
    private val _isSuccess = MutableStateFlow(false)
    private val _saveError = MutableStateFlow<String?>(null)

    private val _validationState = MutableStateFlow(EditEmployeeFormValidation())
    val validationState: StateFlow<EditEmployeeFormValidation> = _validationState.asStateFlow()

    val uiState: StateFlow<EditEmployeeUiState> = if (employeeId == -1) {
        flowOf(EditEmployeeUiState.NotFound)
    } else {
        combine(
            repository.observeEmployeeById(employeeId),
            _isSaving,
            _isSuccess,
            _saveError
        ) { employee, isSaving, isSuccess, saveError ->
            if (employee == null) {
                EditEmployeeUiState.NotFound
            } else {
                EditEmployeeUiState.Content(
                    employee = employee,
                    isSaving = isSaving,
                    isSuccess = isSuccess,
                    saveError = saveError
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EditEmployeeUiState.Loading
    )

    fun updateEmployee(
        name: String,
        departmentIdStr: String,
        branchIdStr: String,
        managerIdStr: String,
        isActive: Boolean
    ) {
        val currentState = uiState.value
        if (currentState !is EditEmployeeUiState.Content || currentState.isSaving || currentState.isSuccess) {
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
        _validationState.value = EditEmployeeFormValidation(
            nameError = nameErr,
            departmentError = deptErr,
            branchError = branchErr,
            managerError = managerErr,
            isValid = isValid
        )

        if (!isValid) return

        _isSaving.value = true
        _saveError.value = null
        viewModelScope.launch {
            try {
                val nowIso = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val currentEmp = currentState.employee

                val updatedEmployee = currentEmp.copy(
                    employeeName = trimmedName,
                    departmentId = deptId,
                    branchId = branchId,
                    managerId = managerId,
                    isActive = isActive,
                    updatedDate = nowIso
                    // createdDate is preserved
                )

                val result = repository.insertEmployee(updatedEmployee)
                when (result) {
                    is Result.Success -> {
                        _isSaving.value = false
                        _isSuccess.value = true
                    }
                    is Result.Error -> {
                        _isSaving.value = false
                        _saveError.value = result.message ?: "Failed to update employee in database"
                    }
                }
            } catch (e: Exception) {
                _isSaving.value = false
                _saveError.value = e.message ?: "An unexpected error occurred while updating employee"
            }
        }
    }

    fun resetError() {
        _saveError.value = null
    }
}
