package com.szopper.data.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncErrorHandler @Inject constructor() {
    
    sealed class SyncError(val message: String, val cause: Throwable? = null) {
        object NoPermissions : SyncError("Required permissions not granted")
        object DeviceNotFound : SyncError("Target device not found")
        object ConnectionFailed : SyncError("Failed to establish connection")
        object ConnectionTimeout : SyncError("Connection timed out")
        object HandshakeFailed : SyncError("Device handshake failed")
        object DataTransferFailed : SyncError("Failed to transfer data")
        object SerializationError : SyncError("Failed to serialize/deserialize data")
        object ConflictResolutionFailed : SyncError("Failed to resolve data conflicts")
        object UnknownDevice : SyncError("Unknown or incompatible device")
        class NetworkError(message: String, cause: Throwable? = null) : SyncError(message, cause)
        class ProtocolError(message: String) : SyncError(message)
        class UnexpectedError(cause: Throwable) : SyncError("Unexpected error occurred", cause)
    }
    
    fun handleConnectionError(exception: Throwable): SyncError {
        return when {
            exception.message?.contains("permission", ignoreCase = true) == true -> SyncError.NoPermissions
            exception.message?.contains("timeout", ignoreCase = true) == true -> SyncError.ConnectionTimeout
            exception.message?.contains("refused", ignoreCase = true) == true -> SyncError.ConnectionFailed
            exception.message?.contains("unreachable", ignoreCase = true) == true -> SyncError.DeviceNotFound
            else -> SyncError.NetworkError("Connection error: ${exception.message}", exception)
        }
    }
    
    fun handleDataTransferError(exception: Throwable): SyncError {
        return when {
            exception.message?.contains("serialization", ignoreCase = true) == true -> SyncError.SerializationError
            exception.message?.contains("protocol", ignoreCase = true) == true -> SyncError.ProtocolError(exception.message ?: "Protocol error")
            exception.message?.contains("timeout", ignoreCase = true) == true -> SyncError.ConnectionTimeout
            else -> SyncError.DataTransferFailed
        }
    }
    
    fun getErrorMessage(error: SyncError): String {
        return when (error) {
            is SyncError.NoPermissions -> "Please grant location and Bluetooth permissions to sync with devices."
            is SyncError.DeviceNotFound -> "The selected device is no longer available. Please try discovering devices again."
            is SyncError.ConnectionFailed -> "Could not connect to the device. Make sure both devices are nearby and have sync enabled."
            is SyncError.ConnectionTimeout -> "Connection timed out. Please check your connection and try again."
            is SyncError.HandshakeFailed -> "Failed to establish secure connection with the device."
            is SyncError.DataTransferFailed -> "Failed to transfer shopping list data. Please try again."
            is SyncError.SerializationError -> "Error processing shopping list data. Please restart the app and try again."
            is SyncError.ConflictResolutionFailed -> "Could not merge shopping lists. Please try again."
            is SyncError.UnknownDevice -> "The device is not compatible with Szopper sync."
            is SyncError.NetworkError -> "Network error: ${error.message}"
            is SyncError.ProtocolError -> "Communication error: ${error.message}"
            is SyncError.UnexpectedError -> "An unexpected error occurred. Please try again."
        }
    }
    
    fun isRetryable(error: SyncError): Boolean {
        return when (error) {
            is SyncError.NoPermissions -> false
            is SyncError.DeviceNotFound -> true
            is SyncError.ConnectionFailed -> true
            is SyncError.ConnectionTimeout -> true
            is SyncError.HandshakeFailed -> true
            is SyncError.DataTransferFailed -> true
            is SyncError.SerializationError -> false
            is SyncError.ConflictResolutionFailed -> true
            is SyncError.UnknownDevice -> false
            is SyncError.NetworkError -> true
            is SyncError.ProtocolError -> false
            is SyncError.UnexpectedError -> true
        }
    }
    
    fun logError(error: SyncError) {
        // In a real app, this would log to analytics or crash reporting
        println("SyncError: ${error.message}")
        error.cause?.printStackTrace()
    }
}