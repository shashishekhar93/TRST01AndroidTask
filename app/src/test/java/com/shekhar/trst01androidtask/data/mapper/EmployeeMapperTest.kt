package com.shekhar.trst01androidtask.data.mapper

import com.shekhar.trst01androidtask.data.remote.model.DocumentDto
import com.shekhar.trst01androidtask.data.remote.model.EmployeeDetailDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmployeeMapperTest {

    @Test
    fun `toEntityAndDocuments maps valid DTO successfully`() {
        val docDto = DocumentDto(
            documentName = "Aadhaar",
            documentType = "ID",
            isVerified = true,
            employeeId = 101,
            createdDate = "2023-01-01",
            updatedDate = "2023-01-02",
            documentId = 1,
            documentUrl = "url",
            thumbnailUrl = "thumb"
        )
        val dto = EmployeeDetailDto(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 1,
            managerId = 10,
            isActive = true,
            branchId = 5,
            createdDate = "2023-01-01",
            updatedDate = "2023-06-01",
            documents = listOf(docDto)
        )

        val result = dto.toEntityAndDocuments()
        assertNotNull(result)
        assertEquals(101, result?.employee?.employeeId)
        assertEquals("John Doe", result?.employee?.employeeName)
        assertEquals(1, result?.documents?.size)
        assertEquals(1, result?.documents?.get(0)?.documentId)
        assertEquals(101, result?.documents?.get(0)?.employeeId)
    }

    @Test
    fun `toEntityAndDocuments returns null when employeeId is missing or invalid`() {
        val dtoNullId = EmployeeDetailDto(
            employeeId = null,
            employeeName = "No ID",
            departmentId = null,
            managerId = null,
            isActive = null,
            branchId = null,
            createdDate = null,
            updatedDate = null,
            documents = null
        )
        assertNull(dtoNullId.toEntityAndDocuments())

        val dtoInvalidId = dtoNullId.copy(employeeId = -1)
        assertNull(dtoInvalidId.toEntityAndDocuments())

        val dtoZeroId = dtoNullId.copy(employeeId = 0)
        assertNull(dtoZeroId.toEntityAndDocuments())
    }

    @Test
    fun `toEntity skips document when documentId is missing or invalid`() {
        val docDtoInvalid = DocumentDto(
            documentName = "Bad Doc",
            documentType = "ID",
            isVerified = false,
            employeeId = 101,
            createdDate = null,
            updatedDate = null,
            documentId = null,
            documentUrl = null,
            thumbnailUrl = null
        )
        val dto = EmployeeDetailDto(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = null,
            createdDate = null,
            updatedDate = null,
            documents = listOf(docDtoInvalid)
        )

        val result = dto.toEntityAndDocuments()
        assertNotNull(result)
        assertTrue(result?.documents.orEmpty().isEmpty())
    }

    @Test
    fun `toEntityAndDocuments handles null or empty documents safely`() {
        val dto = EmployeeDetailDto(
            employeeId = 101,
            employeeName = "John Doe",
            departmentId = 1,
            managerId = null,
            isActive = true,
            branchId = null,
            createdDate = null,
            updatedDate = null,
            documents = null
        )

        val result = dto.toEntityAndDocuments()
        assertNotNull(result)
        assertTrue(result?.documents.orEmpty().isEmpty())
    }
}
