package com.shekhar.trst01androidtask.data.repository

import androidx.room.withTransaction
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.local.dao.DocumentDao
import com.shekhar.trst01androidtask.data.local.dao.EmployeeDao
import com.shekhar.trst01androidtask.data.local.dao.MapAreaDao
import com.shekhar.trst01androidtask.data.local.database.AppDatabase
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import com.shekhar.trst01androidtask.data.mapper.toEntityAndDocuments
import com.shekhar.trst01androidtask.data.remote.api.EmployeeApiService
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val employeeDao: EmployeeDao,
    private val documentDao: DocumentDao,
    private val mapAreaDao: MapAreaDao,
    private val apiService: EmployeeApiService
) : EmployeeRepository {

    override fun observeEmployees(): Flow<List<EmployeeEntity>> {
        return employeeDao.getAllEmployees()
    }

    override suspend fun refreshEmployees(): Result<Unit> {
        return try {
            val response = apiService.getEmployees()
            if (!response.isSuccessful) {
                return Result.Error(IOException("HTTP error ${response.code()}: ${response.message()}"))
            }

            val body = response.body() ?: return Result.Error(NullPointerException("Response body is null"))
            if (body.success != true) {
                return Result.Error(IllegalStateException("API reported failure: ${body.message}"))
            }

            val dataList = body.data
            if (dataList == null) {
                return Result.Error(NullPointerException("Employee data list is null"))
            }

            val mappedRecords = dataList.mapNotNull { employeeDataDto ->
                employeeDataDto.employeeDetails?.toEntityAndDocuments()
            }

            database.withTransaction {
                for (mapped in mappedRecords) {
                    employeeDao.insertEmployee(mapped.employee)
                    val existingDocs = documentDao.getDocumentsForEmployeeSync(mapped.employee.employeeId)
                    val newDocIds = mapped.documents.map { it.documentId }.toSet()
                    for (oldDoc in existingDocs) {
                        if (oldDoc.documentId !in newDocIds) {
                            documentDao.deleteDocument(oldDoc)
                        }
                    }
                    if (mapped.documents.isNotEmpty()) {
                        documentDao.insertDocuments(mapped.documents)
                    }
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun generateNextEmployeeId(): Int {
        return database.withTransaction {
            val maxId = employeeDao.getMaxEmployeeId() ?: 0
            if (maxId < 1000) 1001 else maxId + 1
        }
    }

    override suspend fun getEmployeeById(employeeId: Int): EmployeeEntity? {
        return employeeDao.getEmployeeById(employeeId)
    }

    override fun observeEmployeeById(employeeId: Int): Flow<EmployeeEntity?> {
        return employeeDao.observeEmployeeById(employeeId)
    }

    override suspend fun insertEmployee(employee: EmployeeEntity): Result<Unit> {
        return try {
            employeeDao.insertEmployee(employee)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteEmployee(employee: EmployeeEntity): Result<Unit> {
        return try {
            employeeDao.deleteEmployee(employee)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteEmployeeById(employeeId: Int): Result<Unit> {
        return try {
            employeeDao.deleteEmployeeById(employeeId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeDocumentsForEmployee(employeeId: Int): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsForEmployee(employeeId)
    }

    override suspend fun getDocumentsForEmployee(employeeId: Int): List<DocumentEntity> {
        return documentDao.getDocumentsForEmployeeSync(employeeId)
    }

    override suspend fun saveDocument(document: DocumentEntity): Result<Unit> {
        return try {
            documentDao.insertDocument(document)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteDocument(document: DocumentEntity): Result<Unit> {
        return try {
            documentDao.deleteDocument(document)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteDocumentById(documentId: Int): Result<Unit> {
        return try {
            documentDao.deleteDocumentById(documentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getMapAreaForEmployee(employeeId: Int): MapAreaEntity? {
        return mapAreaDao.getMapAreaForEmployee(employeeId)
    }

    override fun observeMapAreaForEmployee(employeeId: Int): Flow<MapAreaEntity?> {
        return mapAreaDao.observeMapAreaForEmployee(employeeId)
    }

    override suspend fun saveMapArea(employeeId: Int, coordinates: List<LatLngPoint>): Result<Unit> {
        return try {
            val mapArea = MapAreaEntity(employeeId = employeeId, coordinates = coordinates)
            mapAreaDao.insertMapArea(mapArea)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearMapArea(employeeId: Int): Result<Unit> {
        return try {
            mapAreaDao.deleteMapAreaForEmployee(employeeId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
