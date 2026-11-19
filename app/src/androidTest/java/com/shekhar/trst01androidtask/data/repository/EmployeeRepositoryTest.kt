package com.shekhar.trst01androidtask.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.local.dao.DocumentDao
import com.shekhar.trst01androidtask.data.local.dao.EmployeeDao
import com.shekhar.trst01androidtask.data.local.dao.MapAreaDao
import com.shekhar.trst01androidtask.data.local.database.AppDatabase
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import com.shekhar.trst01androidtask.data.remote.api.EmployeeApiService
import com.shekhar.trst01androidtask.data.remote.model.EmployeeDataDto
import com.shekhar.trst01androidtask.data.remote.model.EmployeeDetailDto
import com.shekhar.trst01androidtask.data.remote.model.EmployeeResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EmployeeRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var employeeDao: EmployeeDao
    private lateinit var documentDao: DocumentDao
    private lateinit var mapAreaDao: MapAreaDao
    private val apiService: EmployeeApiService = mockk()

    private lateinit var repository: EmployeeRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        employeeDao = database.employeeDao()
        documentDao = database.documentDao()
        mapAreaDao = database.mapAreaDao()

        repository = EmployeeRepositoryImpl(
            database = database,
            employeeDao = employeeDao,
            documentDao = documentDao,
            mapAreaDao = mapAreaDao,
            apiService = apiService
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun refreshEmployees_successPersistsData() = runTest {
        val detail = EmployeeDetailDto(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-01-01",
            updatedDate = null,
            documents = emptyList()
        )
        val responseModel = EmployeeResponse(
            success = true,
            status = 200,
            message = "Success",
            data = listOf(EmployeeDataDto(detail))
        )
        coEvery { apiService.getEmployees() } returns Response.success(responseModel)

        val result = repository.refreshEmployees()
        assertTrue(result is Result.Success)

        val savedEmployee = employeeDao.getEmployeeById(101)
        assertNotNull(savedEmployee)
        assertEquals("John Doe", savedEmployee?.employeeName)
    }

    @Test
    fun refreshEmployees_httpFailurePreservesLocalData() = runTest {
        val localEmployee = EmployeeEntity(
            employeeId = 202,
            employeeName = "Local Employee",
            departmentId = 2,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-02-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(localEmployee)

        coEvery { apiService.getEmployees() } returns Response.error(500, "".toResponseBody(null))

        val result = repository.refreshEmployees()
        assertTrue(result is Result.Error)

        val preserved = employeeDao.getEmployeeById(202)
        assertNotNull(preserved)
        assertEquals("Local Employee", preserved?.employeeName)
    }

    @Test
    fun insertEmployee_persistsSuccessfully() = runTest {
        val employee = EmployeeEntity(
            employeeId = 303,
            employeeName = "Test User",
            departmentId = 3,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-03-01",
            updatedDate = null
        )
        val result = repository.insertEmployee(employee)
        assertTrue(result is Result.Success)

        val retrieved = repository.getEmployeeById(303)
        assertNotNull(retrieved)
        assertEquals("Test User", retrieved?.employeeName)
    }

    @Test
    fun saveDocument_persistsSuccessfully() = runTest {
        val employee = EmployeeEntity(
            employeeId = 404,
            employeeName = "Doc Owner",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-04-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(employee)

        val document = DocumentEntity(
            documentId = 10,
            employeeId = 404,
            documentName = "Passport",
            documentType = "ID",
            isVerified = true,
            createdDate = "2023-04-01",
            updatedDate = null,
            documentUrl = "http://example.com/passport.pdf",
            thumbnailUrl = "http://example.com/thumb.jpg"
        )
        val result = repository.saveDocument(document)
        assertTrue(result is Result.Success)

        val docs = repository.getDocumentsForEmployee(404)
        assertEquals(1, docs.size)
        assertEquals("Passport", docs[0].documentName)
    }

    @Test
    fun refreshEmployees_preservesUntouchedMapPolygonData() = runTest {
        val employeeId = 505
        val employee = EmployeeEntity(
            employeeId = employeeId,
            employeeName = "Map User",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-05-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(employee)

        val polygonCoordinates = listOf(
            LatLngPoint(12.9716, 77.5946),
            LatLngPoint(13.0827, 80.2707)
        )
        mapAreaDao.insertMapArea(MapAreaEntity(employeeId = employeeId, coordinates = polygonCoordinates))

        val detail = EmployeeDetailDto(
            employeeId = employeeId,
            employeeName = "Map User Updated",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-05-01",
            updatedDate = null,
            documents = emptyList()
        )
        val responseModel = EmployeeResponse(
            success = true,
            status = 200,
            message = "Success",
            data = listOf(EmployeeDataDto(detail))
        )
        coEvery { apiService.getEmployees() } returns Response.success(responseModel)

        val result = repository.refreshEmployees()
        assertTrue(result is Result.Success)

        val updatedEmployee = employeeDao.getEmployeeById(employeeId)
        assertEquals("Map User Updated", updatedEmployee?.employeeName)

        val preservedMapArea = mapAreaDao.getMapAreaForEmployee(employeeId)
        assertNotNull(preservedMapArea)
        assertEquals(2, preservedMapArea?.coordinates?.size)
        assertEquals(12.9716, preservedMapArea?.coordinates?.get(0)?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun generateNextEmployeeId_calculatesNonConflictingId() = runTest {
        val firstGeneratedId = repository.generateNextEmployeeId()
        assertEquals(1001, firstGeneratedId)

        val employee1 = EmployeeEntity(
            employeeId = firstGeneratedId,
            employeeName = "Emp 1",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-01-01",
            updatedDate = null
        )
        repository.insertEmployee(employee1)

        val secondGeneratedId = repository.generateNextEmployeeId()
        assertEquals(1002, secondGeneratedId)
    }

    @Test
    fun deleteEmployeeById_cascadesDeletionOfAssociatedDocumentsAndMapArea() = runTest {
        val empId = 606
        val employee = EmployeeEntity(
            employeeId = empId,
            employeeName = "Delete Cascade Target",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = 1,
            createdDate = "2023-06-01",
            updatedDate = null
        )
        repository.insertEmployee(employee)

        val doc = DocumentEntity(
            documentId = 60,
            employeeId = empId,
            documentName = "ID Card",
            documentType = "ID",
            isVerified = true,
            createdDate = "2023-06-01",
            updatedDate = null,
            documentUrl = "http://example.com/doc",
            thumbnailUrl = null
        )
        repository.saveDocument(doc)

        val mapArea = MapAreaEntity(
            employeeId = empId,
            coordinates = listOf(LatLngPoint(10.0, 20.0))
        )
        mapAreaDao.insertMapArea(mapArea)

        val deleteResult = repository.deleteEmployeeById(empId)
        assertTrue(deleteResult is Result.Success)

        Assert.assertNull(repository.getEmployeeById(empId))
        assertTrue(repository.getDocumentsForEmployee(empId).isEmpty())
        Assert.assertNull(repository.getMapAreaForEmployee(empId))
    }
}
