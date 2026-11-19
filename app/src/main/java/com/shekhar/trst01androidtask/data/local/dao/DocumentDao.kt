package com.shekhar.trst01androidtask.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE employeeId = :employeeId")
    fun getDocumentsForEmployee(employeeId: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE employeeId = :employeeId")
    suspend fun getDocumentsForEmployeeSync(employeeId: Int): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE documentId = :documentId")
    suspend fun deleteDocumentById(documentId: Int)

    @Query("DELETE FROM documents WHERE employeeId = :employeeId")
    suspend fun deleteDocumentsForEmployee(employeeId: Int)
}
