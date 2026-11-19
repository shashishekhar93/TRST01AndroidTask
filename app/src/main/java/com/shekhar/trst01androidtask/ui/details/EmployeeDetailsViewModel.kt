package com.shekhar.trst01androidtask.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EmployeeDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1

    val uiState: StateFlow<EmployeeDetailsUiState> = if (employeeId == -1) {
        flowOf(EmployeeDetailsUiState.NotFound)
    } else {
        combine(
            repository.observeEmployeeById(employeeId),
            repository.observeDocumentsForEmployee(employeeId)
        ) { employee, documents ->
            if (employee == null) {
                EmployeeDetailsUiState.NotFound
            } else {
                EmployeeDetailsUiState.Success(employee, documents)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EmployeeDetailsUiState.Loading
    )
}
