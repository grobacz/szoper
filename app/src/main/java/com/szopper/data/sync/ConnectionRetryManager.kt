package com.szopper.data.sync

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRetryManager @Inject constructor() {
    
    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 10000,
        val backoffMultiplier: Double = 2.0
    )
    
    suspend fun <T> executeWithRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend () -> T?
    ): T? {
        var currentDelay = config.initialDelayMs
        
        repeat(config.maxRetries) { attempt ->
            try {
                val result = operation()
                if (result != null) {
                    return result
                }
            } catch (e: Exception) {
                // Log the exception if needed
                if (attempt == config.maxRetries - 1) {
                    // Last attempt failed, don't delay
                    return null
                }
            }
            
            // Wait before next retry
            delay(currentDelay)
            
            // Exponential backoff
            currentDelay = minOf(
                (currentDelay * config.backoffMultiplier).toLong(),
                config.maxDelayMs
            )
        }
        
        return null
    }
    
    suspend fun executeWithRetryBoolean(
        config: RetryConfig = RetryConfig(),
        operation: suspend () -> Boolean
    ): Boolean {
        var currentDelay = config.initialDelayMs
        
        repeat(config.maxRetries) { attempt ->
            try {
                if (operation()) {
                    return true
                }
            } catch (e: Exception) {
                // Log the exception if needed
                if (attempt == config.maxRetries - 1) {
                    // Last attempt failed
                    return false
                }
            }
            
            // Wait before next retry
            delay(currentDelay)
            
            // Exponential backoff
            currentDelay = minOf(
                (currentDelay * config.backoffMultiplier).toLong(),
                config.maxDelayMs
            )
        }
        
        return false
    }
}