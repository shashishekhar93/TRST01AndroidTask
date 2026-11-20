package com.shekhar.trst01androidtask.ui

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EmployeeUiState {
    data object Loading : EmployeeUiState
    data class Success(val employees: List<EmployeeEntity>, val isRefreshing: Boolean = false) : EmployeeUiState
    data object Empty : EmployeeUiState
    data class Error(val message: String, val employees: List<EmployeeEntity> = emptyList()) : EmployeeUiState
}

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val repository: EmployeeRepository
) : ViewModel() {

    private val _isInitialLoading = MutableStateFlow(true)
    private val _hasReceivedFirstEmission = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    // Observe Room DB source of truth eagerly
    val allEmployees: StateFlow<List<EmployeeEntity>> = repository.observeEmployees()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Filtered employee list based on local search query
    val employees: StateFlow<List<EmployeeEntity>> = combine(
        allEmployees,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { employee ->
                val name = employee.employeeName.orEmpty()
                val dept = employee.departmentId?.toString().orEmpty()
                val idStr = employee.employeeId.toString()
                name.contains(query, ignoreCase = true) ||
                dept.contains(query, ignoreCase = true) ||
                idStr.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Comprehensive UI State handling Scenarios A through G
    val uiState: StateFlow<EmployeeUiState> = combine(
        allEmployees,
        _isInitialLoading,
        _hasReceivedFirstEmission,
        _isRefreshing,
        _error
    ) { rawEmployees, initialLoading, firstEmission, refreshing, err ->
        when {
            (initialLoading || !firstEmission) && rawEmployees.isEmpty() -> EmployeeUiState.Loading

            rawEmployees.isNotEmpty() -> EmployeeUiState.Success(rawEmployees, refreshing)

            !initialLoading && firstEmission && rawEmployees.isEmpty() && err != null ->
                EmployeeUiState.Error(err, emptyList())

            !initialLoading && firstEmission && rawEmployees.isEmpty() ->
                EmployeeUiState.Empty

            else -> EmployeeUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EmployeeUiState.Loading
    )

    init {
        viewModelScope.launch {
            repository.observeEmployees().collect { list ->
                if (!_hasReceivedFirstEmission.value) {
                    _hasReceivedFirstEmission.value = true
                }
                if (_isInitialLoading.value && list.isNotEmpty()) {
                    _isInitialLoading.value = false
                }
            }
        }

        refreshInitial()
    }

    private var hasTriggeredInitialRefresh = false

    private fun refreshInitial() {
        if (hasTriggeredInitialRefresh) return
        hasTriggeredInitialRefresh = true

        viewModelScope.launch {
            _isInitialLoading.value = true
            _error.value = null
            try {
                val result = repository.refreshEmployees()
                if (result is Result.Error) {
                    _error.value = result.message ?: "Failed to refresh employees"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh employees"
            } finally {
                _isInitialLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                val result = repository.refreshEmployees()
                if (result is Result.Error) {
                    _error.value = result.message ?: "Failed to refresh employees"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh employees"
            } finally {
                _isRefreshing.value = false
                _isInitialLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _error.value = null
    }

    fun addEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            try {
                repository.insertEmployee(employee)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add employee"
            }
        }
    }

    fun updateEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            try {
                repository.updateEmployee(employee)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update employee"
            }
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            try {
                repository.deleteEmployee(employee)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete employee"
            }
        }
    }

    fun deleteEmployeeById(employeeId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteEmployeeById(employeeId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete employee"
            }
        }
    }
}
