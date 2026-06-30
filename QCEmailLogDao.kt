package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QCEmailLog
import kotlinx.coroutines.flow.Flow

@Dao
interface QCEmailLogDao {
    @Query("SELECT * FROM qc_email_logs ORDER BY receivedTimestamp DESC")
    fun getAllLogs(): Flow<List<QCEmailLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: QCEmailLog): Long

    @Update
    suspend fun updateLog(log: QCEmailLog)

    @Query("DELETE FROM qc_email_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM qc_email_logs")
    suspend fun clearAllLogs()
}
