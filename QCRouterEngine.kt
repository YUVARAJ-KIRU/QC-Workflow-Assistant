package com.example.data.engine

import com.example.data.model.QCEmailLog

object QCRouterEngine {
    
    /**
     * Processes inbound payload/email against specific compliance keywords.
     * Wraps execution in robust try-catch blocks to ensure no crash and logs errors.
     */
    fun processEmail(
        sender: String,
        ccList: String,
        subject: String,
        body: String
    ): QCEmailLog {
        try {
            // Validation step to demonstrate robust try-catch error logging
            if (sender.isBlank()) {
                throw IllegalArgumentException("Sender email address cannot be empty or blank")
            }
            if (!sender.contains("@")) {
                throw IllegalArgumentException("Invalid email format for Sender: '$sender' (missing '@')")
            }
            
            val subjectLower = subject.lowercase().trim()
            val bodyLower = body.lowercase().trim()
            
            // Path A keywords
            val defectKeywords = listOf(
                "defect", "not working", "issue", "broken", "fault", 
                "malfunction", "deviation", "rejection", "qc failure"
            )
            
            // Path B keywords
            val feedbackKeywords = listOf(
                "feedback", "satisfied", "good", "bad", "improvement", 
                "review", "experience", "suggestion", "customer response"
            )
            
            // Match evaluation
            val hasDefect = defectKeywords.any { subjectLower.contains(it) || bodyLower.contains(it) }
            val hasFeedback = feedbackKeywords.any { subjectLower.contains(it) || bodyLower.contains(it) }
            
            val route: String
            val tag: String
            val draftedReply: String
            
            if (hasDefect) {
                route = "CRITICAL_DEFECT"
                tag = "Important-1-Critical-Defect"
                draftedReply = "Dear Valued Customer,\n\nThank you for contacting our Quality Control department regarding the reported operational deviation.\n\nWe confirm that your ticket has been successfully logged into our QC management system. Our engineering team is currently conducting a comprehensive Root Cause Analysis (RCA) to isolate the technical bottleneck. A 100% verified corrective action and permanent technical solution will be communicated to you within the next 24 hours.\n\nSincerely,\nQuality Control Automation Engine"
            } else if (hasFeedback) {
                route = "CUSTOMER_FEEDBACK"
                tag = "Important-2-Customer-Feedback"
                draftedReply = "Dear Valued Customer,\n\nThank you for taking the time to share your valuable performance feedback with our team.\n\nCustomer insights are integral to our continuous quality improvement processes. Your evaluations have been routed directly to our Quality Assurance specialists to help us refine and elevate our production standards.\n\nBest Regards,\nQuality Control Assurance Team"
            } else {
                route = "GENERAL_QUERY"
                tag = "Important-3-General-Query"
                draftedReply = "Dear Valued Customer,\n\nThank you for reaching out to our Quality Control team.\n\nWe have received your general inquiry and it is currently being routed to the appropriate representative for further review. You can expect a response within 48 business hours.\n\nBest Regards,\nQuality Control Team"
            }
            
            return QCEmailLog(
                sender = sender.trim(),
                ccList = ccList.trim(),
                subject = subject.trim(),
                body = body.trim(),
                receivedTimestamp = System.currentTimeMillis(),
                route = route,
                tag = tag,
                draftedReply = draftedReply,
                status = "PROCESSED"
            )
            
        } catch (e: Exception) {
            // Return error object with status FAILED and captured error message
            return QCEmailLog(
                sender = sender,
                ccList = ccList,
                subject = subject,
                body = body,
                receivedTimestamp = System.currentTimeMillis(),
                route = "ERROR",
                tag = "Important-3-General-Query",
                draftedReply = "System processing failed. Error: ${e.localizedMessage}",
                status = "FAILED",
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }
}
