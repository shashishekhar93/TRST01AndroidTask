package com.shekhar.trst01androidtask.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["employeeId"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["employeeId"])]
)
data class DocumentEntity(
    @PrimaryKey
    val documentId: Int,
    val employeeId: Int,
    val documentName: String?,
    val documentType: String?,
    val isVerified: Boolean?,
    val createdDate: String?,
    val updatedDate: String?,
    val documentUrl: String?,
    val thumbnailUrl: String?
)
