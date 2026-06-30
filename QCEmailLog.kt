package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qc_email_logs")
data class QCEmailLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val ccList: String,
    val subject: String,
    val body: String,
    val receivedTimestamp: Long,
    val route: String,       // "CRITICAL_DEFECT", "CUSTOMER_FEEDBACK", "GENERAL_QUERY", "ERROR"
    val tag: String,         // "Important-1-Critical-Defect", "Important-2-Customer-Feedback", "Important-3-General-Query"
    val draftedReply: String,
    val status: String,      // "PROCESSED", "DISPATCHED", "FAILED"
    val errorMessage: String? = null
)
