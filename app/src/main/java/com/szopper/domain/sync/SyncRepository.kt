package com.szopper.domain.sync

import com.szopper.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun discoverDevices(): Flow<List<DeviceInfo>>
    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean
    suspend fun sendProducts(products: List<Product>): Boolean
    suspend fun receiveProducts(): List<Product>
    suspend fun disconnect()
    fun getConnectionStatus(): Flow<ConnectionStatus>
}

enum class ConnectionStatus {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    SYNCING,
    ERROR
}