package com.shekhar.trst01androidtask.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shekhar.trst01androidtask.data.local.converter.CoordinateConverter
import com.shekhar.trst01androidtask.data.local.dao.DocumentDao
import com.shekhar.trst01androidtask.data.local.dao.EmployeeDao
import com.shekhar.trst01androidtask.data.local.dao.MapAreaDao
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity

@Database(
    entities = [
        EmployeeEntity::class,
        DocumentEntity::class,
        MapAreaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(CoordinateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun documentDao(): DocumentDao
    abstract fun mapAreaDao(): MapAreaDao

    companion object {
        const val DATABASE_NAME = "trst01_employee_database"
    }
}
