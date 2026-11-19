package com.shekhar.trst01androidtask.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.data.local.dao.DocumentDao
import com.shekhar.trst01androidtask.data.local.dao.EmployeeDao
import com.shekhar.trst01androidtask.data.local.dao.MapAreaDao
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var employeeDao: EmployeeDao
    private lateinit var documentDao: DocumentDao
    private lateinit var mapAreaDao: MapAreaDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        employeeDao = database.employeeDao()
        documentDao = database.documentDao()
        mapAreaDao = database.mapAreaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveEmployee() = runBlocking {
        val employee = EmployeeEntity(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 5,
            managerId = 1,
            isActive = true,
            branchId = 2,
            createdDate = "2023-01-01",
            updatedDate = "2023-06-01"
        )

        employeeDao.insertEmployee(employee)
        val retrieved = employeeDao.getEmployeeById(101)

        assertNotNull(retrieved)
        assertEquals("John Doe", retrieved?.employeeName)
        assertEquals(true, retrieved?.isActive)
    }

    @Test
    fun retrieveDocumentsByEmployee() = runBlocking {
        val employee = EmployeeEntity(
            employeeId = 102,
            employeeName = "Jane Smith",
            departmentId = 3,
            managerId = 1,
            isActive = true,
            branchId = 1,
            createdDate = "2023-02-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(employee)

        val doc1 = DocumentEntity(
            documentId = 1,
            employeeId = 102,
            documentName = "Aadhaar",
            documentType = "ID",
            isVerified = true,
            createdDate = "2023-02-01",
            updatedDate = null,
            documentUrl = "http://example.com/aadhaar.pdf",
            thumbnailUrl = "http://example.com/aadhaar_thumb.jpg"
        )
        val doc2 = DocumentEntity(
            documentId = 2,
            employeeId = 102,
            documentName = "PAN",
            documentType = "ID",
            isVerified = false,
            createdDate = "2023-02-01",
            updatedDate = null,
            documentUrl = "http://example.com/pan.pdf",
            thumbnailUrl = "http://example.com/pan_thumb.jpg"
        )

        documentDao.insertDocument(doc1)
        documentDao.insertDocument(doc2)

        val docs = documentDao.getDocumentsForEmployeeSync(102)
        assertEquals(2, docs.size)
        assertEquals("Aadhaar", docs[0].documentName)
        assertEquals("PAN", docs[1].documentName)
    }

    @Test
    fun insertAndRetrieveMapArea() = runBlocking {
        val employee = EmployeeEntity(
            employeeId = 103,
            employeeName = "Bob Builder",
            departmentId = 4,
            managerId = 1,
            isActive = true,
            branchId = 1,
            createdDate = "2023-03-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(employee)

        val coordinates = listOf(
            LatLngPoint(10.0, 20.0),
            LatLngPoint(15.0, 25.0),
            LatLngPoint(12.0, 22.0)
        )
        val mapArea = MapAreaEntity(
            employeeId = 103,
            coordinates = coordinates
        )

        mapAreaDao.insertMapArea(mapArea)
        val retrieved = mapAreaDao.getMapAreaForEmployee(103)

        assertNotNull(retrieved)
        assertEquals(3, retrieved?.coordinates?.size)
        assertEquals(10.0, retrieved?.coordinates?.get(0)?.latitude ?: 0.0, 0.0001)
        assertEquals(25.0, retrieved?.coordinates?.get(1)?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun cascadingDeletionOfDocumentsAndMapAreaWhenEmployeeDeleted() = runBlocking {
        val employeeId = 104
        val employee = EmployeeEntity(
            employeeId = employeeId,
            employeeName = "Alice Delete",
            departmentId = 1,
            managerId = 1,
            isActive = true,
            branchId = 1,
            createdDate = "2023-04-01",
            updatedDate = null
        )
        employeeDao.insertEmployee(employee)

        val document = DocumentEntity(
            documentId = 10,
            employeeId = employeeId,
            documentName = "Resume",
            documentType = "PDF",
            isVerified = true,
            createdDate = "2023-04-01",
            updatedDate = null,
            documentUrl = "url",
            thumbnailUrl = "thumb"
        )
        documentDao.insertDocument(document)

        val mapArea = MapAreaEntity(
            employeeId = employeeId,
            coordinates = listOf(LatLngPoint(1.0, 2.0))
        )
        mapAreaDao.insertMapArea(mapArea)

        // Verify entities exist before deletion
        assertNotNull(employeeDao.getEmployeeById(employeeId))
        assertEquals(1, documentDao.getDocumentsForEmployeeSync(employeeId).size)
        assertNotNull(mapAreaDao.getMapAreaForEmployee(employeeId))

        // Delete employee
        employeeDao.deleteEmployee(employee)

        // Verify employee, documents, and map area are deleted (cascade)
        assertNull(employeeDao.getEmployeeById(employeeId))
        assertTrue(documentDao.getDocumentsForEmployeeSync(employeeId).isEmpty())
        assertNull(mapAreaDao.getMapAreaForEmployee(employeeId))
    }
}
