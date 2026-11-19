package com.shekhar.trst01androidtask.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapAreaDao {

    @Query("SELECT * FROM map_areas WHERE employeeId = :employeeId")
    suspend fun getMapAreaForEmployee(employeeId: Int): MapAreaEntity?

    @Query("SELECT * FROM map_areas WHERE employeeId = :employeeId")
    fun observeMapAreaForEmployee(employeeId: Int): Flow<MapAreaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapArea(mapArea: MapAreaEntity)

    @Query("DELETE FROM map_areas WHERE employeeId = :employeeId")
    suspend fun deleteMapAreaForEmployee(employeeId: Int)
}
