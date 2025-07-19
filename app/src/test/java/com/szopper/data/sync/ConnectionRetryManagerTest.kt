package com.szopper.data.sync

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionRetryManagerTest {

    private lateinit var connectionRetryManager: ConnectionRetryManager

    @Before
    fun setup() {
        connectionRetryManager = ConnectionRetryManager()
    }

    @Test
    fun `executeWithRetry should return result on first success`() = runTest {
        // Given
        val expectedResult = "success"
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetry {
            attemptCount++
            expectedResult
        }

        // Then
        assertEquals(expectedResult, result)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `executeWithRetry should retry on null result`() = runTest {
        // Given
        val config = ConnectionRetryManager.RetryConfig(maxRetries = 3, initialDelayMs = 1)
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetry(config) {
            attemptCount++
            if (attemptCount < 3) null else "success"
        }

        // Then
        assertEquals("success", result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetry should return null after max retries`() = runTest {
        // Given
        val config = ConnectionRetryManager.RetryConfig(maxRetries = 2, initialDelayMs = 1)
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetry(config) {
            attemptCount++
            null // Always fail
        }

        // Then
        assertNull(result)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `executeWithRetry should handle exceptions and retry`() = runTest {
        // Given
        val config = ConnectionRetryManager.RetryConfig(maxRetries = 3, initialDelayMs = 1)
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetry(config) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Connection failed")
            } else {
                "success"
            }
        }

        // Then
        assertEquals("success", result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetryBoolean should return true on success`() = runTest {
        // Given
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetryBoolean {
            attemptCount++
            true
        }

        // Then
        assertTrue(result)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `executeWithRetryBoolean should retry on false result`() = runTest {
        // Given
        val config = ConnectionRetryManager.RetryConfig(maxRetries = 3, initialDelayMs = 1)
        var attemptCount = 0

        // When
        val result = connectionRetryManager.executeWithRetryBoolean(config) {
            attemptCount++
            attemptCount >= 3 // Success on third attempt
        }

        // Then
        assertTrue(result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetryBoolean should return false after max retries`() = runTest {
        // Given
        val config = ConnectionRetryManager.RetryConfig(maxRetries = 2, initialDelayMs = 1)

        // When
        val result = connectionRetryManager.executeWithRetryBoolean(config) {
            false // Always fail
        }

        // Then
        assertFalse(result)
    }
}