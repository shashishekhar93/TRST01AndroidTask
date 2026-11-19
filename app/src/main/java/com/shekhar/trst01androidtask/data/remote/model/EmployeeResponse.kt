package com.shekhar.trst01androidtask.data.remote.model

import com.google.gson.annotations.SerializedName

data class EmployeeResponse(
    @SerializedName("success")
    val success: Boolean?,
    @SerializedName("status")
    val status: Int?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: List<EmployeeDataDto>?
)
