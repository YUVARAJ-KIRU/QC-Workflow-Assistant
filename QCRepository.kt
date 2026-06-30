package com.example.data.repository

import com.example.data.local.QCEmailLogDao
import com.example.data.model.QCEmailLog
import kotlinx.coroutines.flow.Flow

class QCRepository(private val dao: QCEmailLogDao) {
    val allLogs: Flow<List<QCEmailLog>> = dao.getAllLogs()

    suspend fun insertLog(log: QCEmailLog): Long {
        return dao.insertLog(log)
    }

    suspend fun updateLog(log: QCEmailLog) {
        dao.updateLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        dao.deleteLogById(id)
    }

    suspend fun clearAllLogs() {
        dao.clearAllLogs()
    }
}
