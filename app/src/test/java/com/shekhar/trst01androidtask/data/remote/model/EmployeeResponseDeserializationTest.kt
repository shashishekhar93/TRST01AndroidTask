package com.shekhar.trst01androidtask.data.remote.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmployeeResponseDeserializationTest {

    private val gson = Gson()

    @Test
    fun `test normal complete response with multiple and two documents`() {
        val json = """
            {
                "success": true,
                "status": 200,
                "message": "Success",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 101,
                            "EmployeeName": "John Doe",
                            "DepartmentId": 1,
                            "ManagerId": 10,
                            "IsActive": true,
                            "BranchId": 5,
                            "CreatedDate": "2023-01-01T00:00:00Z",
                            "UpdatedDate": "2023-06-01T00:00:00Z",
                            "Documents": [
                                {
                                    "DocumentName": "Aadhaar",
                                    "DocumentType": "ID",
                                    "IsVerified": true,
                                    "EmployeeId": 101,
                                    "CreatedDate": "2023-01-01T00:00:00Z",
                                    "UpdatedDate": "2023-01-01T00:00:00Z",
                                    "DocumentId": 1,
                                    "DocumentUrl": "https://example.com/aadhaar.pdf",
                                    "ThumbnailUrl": "https://example.com/aadhaar_thumb.jpg"
                                },
                                {
                                    "DocumentName": "PAN",
                                    "DocumentType": "ID",
                                    "IsVerified": false,
                                    "EmployeeId": 101,
                                    "CreatedDate": "2023-01-02T00:00:00Z",
                                    "UpdatedDate": "2023-01-02T00:00:00Z",
                                    "DocumentId": 2,
                                    "DocumentUrl": "https://example.com/pan.pdf",
                                    "ThumbnailUrl": "https://example.com/pan_thumb.jpg"
                                },
                                {
                                    "DocumentName": "Passport",
                                    "DocumentType": "Travel",
                                    "IsVerified": true,
                                    "EmployeeId": 101,
                                    "CreatedDate": "2023-01-03T00:00:00Z",
                                    "UpdatedDate": "2023-01-03T00:00:00Z",
                                    "DocumentId": 3,
                                    "DocumentUrl": "https://example.com/passport.pdf",
                                    "ThumbnailUrl": "https://example.com/passport_thumb.jpg"
                                }
                            ]
                        }
                    },
                    {
                        "employee_details": {
                            "EmployeeId": 102,
                            "EmployeeName": "Jane Smith",
                            "DepartmentId": 2,
                            "ManagerId": 10,
                            "IsActive": true,
                            "BranchId": 5,
                            "CreatedDate": "2023-02-01T00:00:00Z",
                            "UpdatedDate": "2023-06-01T00:00:00Z",
                            "Documents": [
                                {
                                    "DocumentName": "Aadhaar",
                                    "DocumentType": "ID",
                                    "IsVerified": true,
                                    "EmployeeId": 102,
                                    "CreatedDate": "2023-02-01T00:00:00Z",
                                    "UpdatedDate": "2023-02-01T00:00:00Z",
                                    "DocumentId": 4,
                                    "DocumentUrl": "https://example.com/aadhaar2.pdf",
                                    "ThumbnailUrl": "https://example.com/aadhaar2_thumb.jpg"
                                },
                                {
                                    "DocumentName": "VoterId",
                                    "DocumentType": "ID",
                                    "IsVerified": true,
                                    "EmployeeId": 102,
                                    "CreatedDate": "2023-02-02T00:00:00Z",
                                    "UpdatedDate": "2023-02-02T00:00:00Z",
                                    "DocumentId": 5,
                                    "DocumentUrl": "https://example.com/voter.pdf",
                                    "ThumbnailUrl": "https://example.com/voter_thumb.jpg"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, EmployeeResponse::class.java)

        assertNotNull(response)
        assertTrue(response.success == true)
        assertEquals(200, response.status)
        assertEquals("Success", response.message)

        val data = response.data
        assertNotNull(data)
        assertEquals(2, data?.size)

        val emp1 = data?.get(0)?.employeeDetails
        assertNotNull(emp1)
        assertEquals(101, emp1?.employeeId)
        assertEquals("John Doe", emp1?.employeeName)
        assertEquals(3, emp1?.documents?.size)
        assertEquals("Aadhaar", emp1?.documents?.get(0)?.documentName)
        assertEquals("PAN", emp1?.documents?.get(1)?.documentName)
        assertEquals("Passport", emp1?.documents?.get(2)?.documentName)

        val emp2 = data?.get(1)?.employeeDetails
        assertNotNull(emp2)
        assertEquals(102, emp2?.employeeId)
        assertEquals("Jane Smith", emp2?.employeeName)
        assertEquals(2, emp2?.documents?.size)
        assertEquals("Aadhaar", emp2?.documents?.get(0)?.documentName)
        assertEquals("VoterId", emp2?.documents?.get(1)?.documentName)
    }

    @Test
    fun `test explicit null and missing employee name`() {
        val jsonNullName = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 103,
                            "EmployeeName": null
                        }
                    }
                ]
            }
        """.trimIndent()

        val resp1 = gson.fromJson(jsonNullName, EmployeeResponse::class.java)
        assertNull(resp1.data?.get(0)?.employeeDetails?.employeeName)

        val jsonMissingName = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 104
                        }
                    }
                ]
            }
        """.trimIndent()

        val resp2 = gson.fromJson(jsonMissingName, EmployeeResponse::class.java)
        assertNull(resp2.data?.get(0)?.employeeDetails?.employeeName)
    }

    @Test
    fun `test null, missing, and empty documents`() {
        val jsonNullDocs = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 105,
                            "Documents": null
                        }
                    }
                ]
            }
        """.trimIndent()

        val resp1 = gson.fromJson(jsonNullDocs, EmployeeResponse::class.java)
        assertNull(resp1.data?.get(0)?.employeeDetails?.documents)

        val jsonMissingDocs = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 106
                        }
                    }
                ]
            }
        """.trimIndent()

        val resp2 = gson.fromJson(jsonMissingDocs, EmployeeResponse::class.java)
        assertNull(resp2.data?.get(0)?.employeeDetails?.documents)

        val jsonEmptyDocs = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 107,
                            "Documents": []
                        }
                    }
                ]
            }
        """.trimIndent()

        val resp3 = gson.fromJson(jsonEmptyDocs, EmployeeResponse::class.java)
        assertNotNull(resp3.data?.get(0)?.employeeDetails?.documents)
        assertTrue(resp3.data?.get(0)?.employeeDetails?.documents?.isEmpty() == true)
    }

    @Test
    fun `test null and missing employee details and root data`() {
        val jsonNullDetails = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": null
                    }
                ]
            }
        """.trimIndent()

        val resp1 = gson.fromJson(jsonNullDetails, EmployeeResponse::class.java)
        assertNull(resp1.data?.get(0)?.employeeDetails)

        val jsonMissingDetails = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {}
                ]
            }
        """.trimIndent()

        val resp2 = gson.fromJson(jsonMissingDetails, EmployeeResponse::class.java)
        assertNull(resp2.data?.get(0)?.employeeDetails)

        val jsonNullRootData = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": null
            }
        """.trimIndent()

        val resp3 = gson.fromJson(jsonNullRootData, EmployeeResponse::class.java)
        assertNull(resp3.data)
    }

    @Test
    fun `test null document url and missing optional document fields`() {
        val json = """
            {
                "success": true,
                "status": 200,
                "message": "OK",
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": 108,
                            "Documents": [
                                {
                                    "DocumentName": null,
                                    "DocumentUrl": null,
                                    "DocumentId": 10
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, EmployeeResponse::class.java)
        val doc = response.data?.get(0)?.employeeDetails?.documents?.get(0)
        assertNotNull(doc)
        assertEquals(10, doc?.documentId)
        assertNull(doc?.documentName)
        assertNull(doc?.documentUrl)
        assertNull(doc?.documentType)
        assertNull(doc?.isVerified)
    }

    @Test
    fun `test null numeric and boolean fields`() {
        val json = """
            {
                "success": null,
                "status": null,
                "message": null,
                "data": [
                    {
                        "employee_details": {
                            "EmployeeId": null,
                            "DepartmentId": null,
                            "ManagerId": null,
                            "IsActive": null,
                            "BranchId": null
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, EmployeeResponse::class.java)
        assertNull(response.success)
        assertNull(response.status)
        assertNull(response.message)

        val details = response.data?.get(0)?.employeeDetails
        assertNotNull(details)
        assertNull(details?.employeeId)
        assertNull(details?.departmentId)
        assertNull(details?.managerId)
        assertNull(details?.isActive)
        assertNull(details?.branchId)
    }
}
