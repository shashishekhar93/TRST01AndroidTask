package com.shekhar.trst01androidtask.data.remote.model

import com.google.gson.annotations.SerializedName

data class EmployeeDataDto(
    @SerializedName("employee_details")
    val employeeDetails: EmployeeDetailDto?
)
