package com.shekhar.trst01androidtask.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM employees")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE employeeId = :employeeId")
    suspend fun getEmployeeById(employeeId: Int): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE employeeId = :employeeId")
    fun observeEmployeeById(employeeId: Int): Flow<EmployeeEntity?>

    @Query("SELECT MAX(employeeId) FROM employees")
    suspend fun getMaxEmployeeId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    //update employee


    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE employeeId = :employeeId")
    suspend fun deleteEmployeeById(employeeId: Int)

    @Query("DELETE FROM employees")
    suspend fun deleteAllEmployees()
}
