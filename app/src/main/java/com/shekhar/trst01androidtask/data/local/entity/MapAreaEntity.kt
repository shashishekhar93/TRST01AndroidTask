package com.shekhar.trst01androidtask.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint

@Entity(
    tableName = "map_areas",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["employeeId"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MapAreaEntity(
    @PrimaryKey
    val employeeId: Int,
    val coordinates: List<LatLngPoint>
)
