package com.shekhar.trst01androidtask.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import com.shekhar.trst01androidtask.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1

    private val _uiState = MutableStateFlow<EmployeeMapUiState>(EmployeeMapUiState.Loading)
    val uiState: StateFlow<EmployeeMapUiState> = _uiState.asStateFlow()

    private var initialSavedPoints: List<LatLngPoint>? = null
    private var isInitialized = false

    init {
        loadEmployeeAndMapArea()
    }

    private fun loadEmployeeAndMapArea() {
        if (employeeId == -1) {
            _uiState.value = EmployeeMapUiState.NotFound
            return
        }

        viewModelScope.launch {
            val employee = repository.getEmployeeById(employeeId)
            if (employee == null) {
                _uiState.value = EmployeeMapUiState.NotFound
                return@launch
            }

            repository.observeMapAreaForEmployee(employeeId).collectLatest { mapAreaEntity ->
                val savedPoints = mapAreaEntity?.coordinates ?: emptyList()

                if (!isInitialized) {
                    initialSavedPoints = savedPoints
                    isInitialized = true
                    _uiState.value = EmployeeMapUiState.Success(
                        employeeId = employee.employeeId,
                        employeeName = employee.employeeName,
                        points = savedPoints,
                        selectedIndex = null,
                        isSavedInDb = mapAreaEntity != null && mapAreaEntity.coordinates.isNotEmpty(),
                        isSaving = false,
                        isClearing = false,
                        hasUnsavedChanges = false
                    )
                } else {
                    val currentState = _uiState.value
                    if (currentState is EmployeeMapUiState.Success) {
                        _uiState.update {
                            (it as EmployeeMapUiState.Success).copy(
                                isSavedInDb = mapAreaEntity != null && mapAreaEntity.coordinates.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }
    }

    fun addPoint(point: LatLngPoint) {
        if (!isValidCoordinate(point)) {
            _uiState.update { currentState ->
                if (currentState is EmployeeMapUiState.Success) {
                    currentState.copy(errorMessage = "Invalid latitude or longitude coordinate.")
                } else currentState
            }
            return
        }

        _uiState.update { currentState ->
            if (currentState is EmployeeMapUiState.Success) {
                val newPoints = currentState.points + point
                val newIndex = newPoints.lastIndex
                currentState.copy(
                    points = newPoints,
                    selectedIndex = newIndex,
                    validationError = null,
                    userMessage = null,
                    errorMessage = null,
                    hasUnsavedChanges = checkIfUnsavedChanges(newPoints)
                )
            } else currentState
        }
    }

    fun movePoint(index: Int, newPoint: LatLngPoint) {
        if (!isValidCoordinate(newPoint)) {
            _uiState.update { currentState ->
                if (currentState is EmployeeMapUiState.Success) {
                    currentState.copy(errorMessage = "Invalid latitude or longitude coordinate.")
                } else currentState
            }
            return
        }

        _uiState.update { currentState ->
            if (currentState is EmployeeMapUiState.Success) {
                if (index in currentState.points.indices) {
                    val updatedPoints = currentState.points.toMutableList()
                    updatedPoints[index] = newPoint
                    currentState.copy(
                        points = updatedPoints,
                        validationError = null,
                        userMessage = null,
                        errorMessage = null,
                        hasUnsavedChanges = checkIfUnsavedChanges(updatedPoints)
                    )
                } else currentState
            } else currentState
        }
    }

    fun selectPoint(index: Int?) {
        _uiState.update { currentState ->
            if (currentState is EmployeeMapUiState.Success) {
                val validIndex = if (index != null && index in currentState.points.indices) index else null
                currentState.copy(selectedIndex = validIndex)
            } else currentState
        }
    }

    fun removeSelectedPoint() {
        _uiState.update { currentState ->
            if (currentState is EmployeeMapUiState.Success) {
                val indexToRemove = currentState.selectedIndex
                if (indexToRemove != null && indexToRemove in currentState.points.indices) {
                    val newPoints = currentState.points.toMutableList().apply { removeAt(indexToRemove) }
                    currentState.copy(
                        points = newPoints,
                        selectedIndex = null,
                        validationError = null,
                        userMessage = null,
                        errorMessage = null,
                        hasUnsavedChanges = checkIfUnsavedChanges(newPoints)
                    )
                } else currentState
            } else currentState
        }
    }

    fun saveMapArea() {
        val currentState = _uiState.value
        if (currentState !is EmployeeMapUiState.Success) return
        if (currentState.isSaving || currentState.isClearing) return

        if (currentState.points.size < 3) {
            _uiState.update {
                currentState.copy(
                    validationError = "A polygon requires at least 3 points.",
                    errorMessage = null
                )
            }
            return
        }

        for (point in currentState.points) {
            if (!isValidCoordinate(point)) {
                _uiState.update {
                    currentState.copy(
                        errorMessage = "Invalid coordinate range found.",
                        validationError = null
                    )
                }
                return
            }
        }

        _uiState.update {
            currentState.copy(isSaving = true, validationError = null, errorMessage = null, userMessage = null)
        }

        viewModelScope.launch {
            val result = repository.saveMapArea(employeeId, currentState.points)
            when (result) {
                is Result.Success -> {
                    initialSavedPoints = currentState.points
                    _uiState.update { state ->
                        if (state is EmployeeMapUiState.Success) {
                            state.copy(
                                isSaving = false,
                                isSavedInDb = true,
                                hasUnsavedChanges = false,
                                userMessage = "Map area saved successfully."
                            )
                        } else state
                    }
                }
                is Result.Error -> {
                    val msg = result.message ?: "Failed to save map area."
                    _uiState.update { state ->
                        if (state is EmployeeMapUiState.Success) {
                            state.copy(
                                isSaving = false,
                                errorMessage = msg
                            )
                        } else state
                    }
                }
            }
        }
    }

    fun clearMapArea() {
        val currentState = _uiState.value
        if (currentState !is EmployeeMapUiState.Success) return
        if (currentState.isSaving || currentState.isClearing) return

        _uiState.update {
            currentState.copy(isClearing = true, validationError = null, errorMessage = null, userMessage = null)
        }

        viewModelScope.launch {
            val result = repository.clearMapArea(employeeId)
            when (result) {
                is Result.Success -> {
                    initialSavedPoints = emptyList()
                    _uiState.update { state ->
                        if (state is EmployeeMapUiState.Success) {
                            state.copy(
                                isClearing = false,
                                points = emptyList(),
                                selectedIndex = null,
                                isSavedInDb = false,
                                hasUnsavedChanges = false,
                                userMessage = "Map area cleared successfully."
                            )
                        } else state
                    }
                }
                is Result.Error -> {
                    val msg = result.message ?: "Failed to clear map area."
                    _uiState.update { state ->
                        if (state is EmployeeMapUiState.Success) {
                            state.copy(
                                isClearing = false,
                                errorMessage = msg
                            )
                        } else state
                    }
                }
            }
        }
    }

    fun clearUserMessages() {
        _uiState.update { currentState ->
            if (currentState is EmployeeMapUiState.Success) {
                currentState.copy(userMessage = null, errorMessage = null, validationError = null)
            } else currentState
        }
    }

    private fun isValidCoordinate(point: LatLngPoint): Boolean {
        return point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
    }

    private fun checkIfUnsavedChanges(currentPoints: List<LatLngPoint>): Boolean {
        val initial = initialSavedPoints ?: emptyList()
        return currentPoints != initial
    }
}
