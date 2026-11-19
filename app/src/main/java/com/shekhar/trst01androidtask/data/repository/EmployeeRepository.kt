package com.shekhar.trst01androidtask.data.repository

import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import kotlinx.coroutines.flow.Flow

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val message: String? = exception.message) : Result<Nothing>
}

interface EmployeeRepository {
    // Observe local employees
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    // Refresh from API
    suspend fun refreshEmployees(): Result<Unit>

    // Employee CRUD
    suspend fun generateNextEmployeeId(): Int
    suspend fun getEmployeeById(employeeId: Int): EmployeeEntity?
    fun observeEmployeeById(employeeId: Int): Flow<EmployeeEntity?>
    suspend fun insertEmployee(employee: EmployeeEntity): Result<Unit>

    suspend fun deleteEmployee(employee: EmployeeEntity): Result<Unit>
    suspend fun deleteEmployeeById(employeeId: Int): Result<Unit>

    // Document operations
    fun observeDocumentsForEmployee(employeeId: Int): Flow<List<DocumentEntity>>
    suspend fun getDocumentsForEmployee(employeeId: Int): List<DocumentEntity>
    suspend fun saveDocument(document: DocumentEntity): Result<Unit>
    suspend fun deleteDocument(document: DocumentEntity): Result<Unit>
    suspend fun deleteDocumentById(documentId: Int): Result<Unit>

    // Map area operations
    suspend fun getMapAreaForEmployee(employeeId: Int): MapAreaEntity?
    fun observeMapAreaForEmployee(employeeId: Int): Flow<MapAreaEntity?>
    suspend fun saveMapArea(employeeId: Int, coordinates: List<LatLngPoint>): Result<Unit>
    suspend fun clearMapArea(employeeId: Int): Result<Unit>
}
