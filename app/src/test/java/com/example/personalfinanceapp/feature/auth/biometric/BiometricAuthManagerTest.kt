package com.example.personalfinanceapp.feature.auth.biometric

import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAuthManagerTest {

    @Test
    fun biometricStatusEnumValuesMatchExpectations() {
        val available = BiometricStatus.AVAILABLE
        val notEnrolled = BiometricStatus.NOT_ENROLLED
        val notAvailable = BiometricStatus.NOT_AVAILABLE

        assertEquals("AVAILABLE", available.name)
        assertEquals("NOT_ENROLLED", notEnrolled.name)
        assertEquals("NOT_AVAILABLE", notAvailable.name)
    }
}
