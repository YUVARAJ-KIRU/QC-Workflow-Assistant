package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QCDatabase
import com.example.data.engine.QCRouterEngine
import com.example.data.model.QCEmailLog
import com.example.data.repository.QCRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QCViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: QCRepository
    val allLogs: StateFlow<List<QCEmailLog>>
    
    private val _selectedLog = MutableStateFlow<QCEmailLog?>(null)
    val selectedLog: StateFlow<QCEmailLog?> = _selectedLog.asStateFlow()

    init {
        val db = QCDatabase.getDatabase(application)
        repository = QCRepository(db.qcEmailLogDao())
        
        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Auto-seed database if empty to present a beautifully filled interface
        viewModelScope.launch {
            repository.allLogs.collect { logs ->
                if (logs.isEmpty()) {
                    seedDefaultData()
                }
            }
        }
    }

    fun selectLog(log: QCEmailLog) {
        _selectedLog.value = log
    }

    fun deselectLog() {
        _selectedLog.value = null
    }

    fun addManualPayload(sender: String, ccList: String, subject: String, body: String) {
        viewModelScope.launch {
            val processedLog = QCRouterEngine.processEmail(sender, ccList, subject, body)
            val newId = repository.insertLog(processedLog)
            // Automatically select the newly created log for immediate feedback
            _selectedLog.value = processedLog.copy(id = newId.toInt())
        }
    }

    fun simulateEmailArrival(sender: String, ccList: String, subject: String, body: String) {
        viewModelScope.launch {
            val processedLog = QCRouterEngine.processEmail(sender, ccList, subject, body)
            val newId = repository.insertLog(processedLog)
            // If nothing is selected, select the new inbound email
            if (_selectedLog.value == null) {
                _selectedLog.value = processedLog.copy(id = newId.toInt())
            }
        }
    }

    fun dispatchDraftReply(log: QCEmailLog) {
        viewModelScope.launch {
            val updated = log.copy(status = "DISPATCHED")
            repository.updateLog(updated)
            // Update selection in detailed view
            if (_selectedLog.value?.id == log.id) {
                _selectedLog.value = updated
            }
        }
    }

    fun deleteLog(log: QCEmailLog) {
        viewModelScope.launch {
            repository.deleteLogById(log.id)
            if (_selectedLog.value?.id == log.id) {
                _selectedLog.value = null
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            _selectedLog.value = null
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.clearAllLogs()
            seedDefaultData()
            _selectedLog.value = null
        }
    }

    private suspend fun seedDefaultData() {
        val defaults = listOf(
            QCRouterEngine.processEmail(
                sender = "j.smith@aerotech-components.com",
                ccList = "ops@aerotech-components.com, compliance@aerotech-components.com",
                subject = "Urgent: Turbine Blade Micro-fracture Defect Detected",
                body = "During the routine ultrasonic inspection of batch #A984, we identified a critical structural defect on the third stage turbine blade (serial TS-9381). The deviation exceeds our 0.02mm threshold. We are initiating a temporary production line halt. Please confirm receipt and provide corrective action."
            ),
            QCRouterEngine.processEmail(
                sender = "k.miller@precision-machining.org",
                ccList = "procurement@precision-machining.org",
                subject = "Positive Performance Review - Q3 Shipment Feedback",
                body = "I wanted to take a moment to express our satisfaction with the recent delivery. The dimensional accuracy of the shafts is satisfied with perfect tolerance. Your customer response has been good, and we look forward to a continued experience."
            ),
            QCRouterEngine.processEmail(
                sender = "quality-assurance@global-logistics.net",
                ccList = "management@global-logistics.net",
                subject = "Inquiry about standard quality operational procedures",
                body = "Greetings, we are reviewing our incoming supply audit checklist. Could you kindly provide the latest ISO 9001 compliance sheets for your manufacturing facility?"
            ),
            // Demonstrating robust error handling logging with malformed sender email:
            QCRouterEngine.processEmail(
                sender = "invalid_sender_format_error",
                ccList = "devops@qc-hub.io",
                subject = "Automated PLC Sensor Output Log",
                body = "PLC telemetry critical threshold fault detected on station 4B."
            )
        )
        
        for (log in defaults) {
            repository.insertLog(log)
        }
    }
}
