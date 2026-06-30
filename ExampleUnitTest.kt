package com.example

import com.example.data.engine.QCRouterEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying compliance with requirements of the Quality Control Routing Engine.
 */
class ExampleUnitTest {

    @Test
    fun test_CriticalDefectRoute_PathA() {
        // Evaluate typical "defect" keywords
        val log = QCRouterEngine.processEmail(
            sender = "supplier@aerospace.com",
            ccList = "leads@aerospace.com",
            subject = "Issue: Faulty calibration on hydraulic valve",
            body = "The valve exhibits a high deviation malfunction."
        )

        assertEquals("CRITICAL_DEFECT", log.route)
        assertEquals("Important-1-Critical-Defect", log.tag)
        assertTrue(log.draftedReply.contains("Root Cause Analysis (RCA)"))
        assertEquals("PROCESSED", log.status)
        assertNull(log.errorMessage)
    }

    @Test
    fun test_CustomerFeedbackRoute_PathB() {
        // Evaluate "feedback" and "satisfied" keywords
        val log = QCRouterEngine.processEmail(
            sender = "client@chassis.net",
            ccList = "mgmt@chassis.net",
            subject = "Review: Satisfied with Q2 performance",
            body = "Our team has provided excellent feedback on standard operations."
        )

        assertEquals("CUSTOMER_FEEDBACK", log.route)
        assertEquals("Important-2-Customer-Feedback", log.tag)
        assertTrue(log.draftedReply.contains("Customer insights are integral"))
        assertEquals("PROCESSED", log.status)
        assertNull(log.errorMessage)
    }

    @Test
    fun test_GeneralQuery_DefaultPath() {
        // No matching keywords
        val log = QCRouterEngine.processEmail(
            sender = "info@external.org",
            ccList = "archive@external.org",
            subject = "Inquiry regarding standard compliance sheets",
            body = "Could you forward the audit list for this quarter?"
        )

        assertEquals("GENERAL_QUERY", log.route)
        assertEquals("Important-3-General-Query", log.tag)
        assertTrue(log.draftedReply.contains("general inquiry"))
        assertEquals("PROCESSED", log.status)
        assertNull(log.errorMessage)
    }

    @Test
    fun test_RobustExceptionCatch_EmptySender() {
        // Blank sender should throw and be handled by the try-catch block safely
        val log = QCRouterEngine.processEmail(
            sender = "   ",
            ccList = "devops@qc.io",
            subject = "Critical system warning",
            body = "PLC calibration fault"
        )

        assertEquals("ERROR", log.route)
        assertEquals("FAILED", log.status)
        assertNotNull(log.errorMessage)
        assertTrue(log.errorMessage!!.contains("Sender email address cannot be empty"))
    }

    @Test
    fun test_RobustExceptionCatch_InvalidEmail() {
        // Missing '@' format should throw and be handled by the try-catch block safely
        val log = QCRouterEngine.processEmail(
            sender = "no_at_symbol_sender",
            ccList = "admin@qc.io",
            subject = "Faulty relay sensor",
            body = "Overheated"
        )

        assertEquals("ERROR", log.route)
        assertEquals("FAILED", log.status)
        assertNotNull(log.errorMessage)
        assertTrue(log.errorMessage!!.contains("Invalid email format"))
    }
}
