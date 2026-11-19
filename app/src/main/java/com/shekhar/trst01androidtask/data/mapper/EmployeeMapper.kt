package com.shekhar.trst01androidtask.data.mapper

import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.remote.model.DocumentDto
import com.shekhar.trst01androidtask.data.remote.model.EmployeeDetailDto

data class MappedEmployeeData(
    val employee: EmployeeEntity,
    val documents: List<DocumentEntity>
)

fun EmployeeDetailDto.toEntityAndDocuments(): MappedEmployeeData? {
    val id = employeeId ?: return null
    if (id <= 0) return null

    val employeeEntity = EmployeeEntity(
        employeeId = id,
        employeeName = employeeName,
        departmentId = departmentId,
        managerId = managerId,
        isActive = isActive,
        branchId = branchId,
        createdDate = createdDate,
        updatedDate = updatedDate
    )

    val docEntities = documents.orEmpty().mapNotNull { docDto ->
        docDto.toEntity(fallbackEmployeeId = id)
    }

    return MappedEmployeeData(employeeEntity, docEntities)
}

fun DocumentDto.toEntity(fallbackEmployeeId: Int): DocumentEntity? {
    val docId = documentId ?: return null
    if (docId <= 0) return null

    val empId = employeeId ?: fallbackEmployeeId

    return DocumentEntity(
        documentId = docId,
        employeeId = empId,
        documentName = documentName,
        documentType = documentType,
        isVerified = isVerified,
        createdDate = createdDate,
        updatedDate = updatedDate,
        documentUrl = documentUrl,
        thumbnailUrl = thumbnailUrl
    )
}
