package com.shekhar.trst01androidtask.data.remote.model

import com.google.gson.annotations.SerializedName

data class DocumentDto(
    @SerializedName("DocumentName")
    val documentName: String?,
    @SerializedName("DocumentType")
    val documentType: String?,
    @SerializedName("IsVerified")
    val isVerified: Boolean?,
    @SerializedName("EmployeeId")
    val employeeId: Int?,
    @SerializedName("CreatedDate")
    val createdDate: String?,
    @SerializedName("UpdatedDate")
    val updatedDate: String?,
    @SerializedName("DocumentId")
    val documentId: Int?,
    @SerializedName("DocumentUrl")
    val documentUrl: String?,
    @SerializedName("ThumbnailUrl")
    val thumbnailUrl: String?
)
