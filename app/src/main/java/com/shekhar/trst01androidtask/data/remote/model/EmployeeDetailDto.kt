package com.shekhar.trst01androidtask.data.remote.model

import com.google.gson.annotations.SerializedName

data class EmployeeDetailDto(
    @SerializedName("EmployeeId")
    val employeeId: Int?,
    @SerializedName("EmployeeName")
    val employeeName: String?,
    @SerializedName("DepartmentId")
    val departmentId: Int?,
    @SerializedName("ManagerId")
    val managerId: Int?,
    @SerializedName("IsActive")
    val isActive: Boolean?,
    @SerializedName("BranchId")
    val branchId: Int?,
    @SerializedName("CreatedDate")
    val createdDate: String?,
    @SerializedName("UpdatedDate")
    val updatedDate: String?,
    @SerializedName("Documents")
    val documents: List<DocumentDto>?
)
