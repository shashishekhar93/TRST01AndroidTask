package com.shekhar.trst01androidtask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey
    val employeeId: Int,
    val employeeName: String?,
    val departmentId: Int?,
    val managerId: Int?,
    val isActive: Boolean?,
    val branchId: Int?,
    val createdDate: String?,
    val updatedDate: String?
)
