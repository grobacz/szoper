package com.szopper.data.sync

import com.szopper.data.sync.wifi.WifiDirectManager
import com.szopper.domain.model.Product
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val wifiDirectManager: WifiDirectManager,
    private val dataTransferManager: DataTransferManager,
    private val connectionRetryManager: ConnectionRetryManager,
    private val syncErrorHandler: SyncErrorHandler
) : SyncRepository {
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private var currentConnection: DeviceInfo? = null
    
    override fun discoverDevices(): Flow<List<DeviceInfo>> {
        android.util.Log.d("SyncRepositoryImpl", "Starting device discovery with connection check")
        _connectionStatus.value = ConnectionStatus.DISCOVERING
        
        return flow {
            // First, check for existing WiFi Direct connections
            android.util.Log.d("SyncRepositoryImpl", "Checking existing WiFi Direct connections first")
            
            try {
                var foundExistingConnections = false
                
                wifiDirectManager.checkExistingConnections().collect { connectedDevices ->
                    android.util.Log.d("SyncRepositoryImpl", "Connection check returned ${connectedDevices.size} devices")
                    emit(connectedDevices)
                    
                    if (connectedDevices.isNotEmpty()) {
                        android.util.Log.i("SyncRepositoryImpl", "✓ Found ${connectedDevices.size} existing connections - skipping discovery")
                        foundExistingConnections = true
                    } else {
                        android.util.Log.d("SyncRepositoryImpl", "No existing connections found")
                    }
                }
                
                // Only start discovery if no existing connections were found
                if (!foundExistingConnections) {
                    android.util.Log.d("SyncRepositoryImpl", "Starting normal discovery since no existing connections")
                    try {
                        wifiDirectManager.discoverPeers().collect { discoveredDevices ->
                            android.util.Log.d("SyncRepositoryImpl", "Discovery returned ${discoveredDevices.size} devices")
                            emit(discoveredDevices)
                        }
                    } catch (discoveryException: Exception) {
                        android.util.Log.w("SyncRepositoryImpl", "Discovery failed but that's OK: ${discoveryException.message}")
                        // Don't emit error for discovery failure - just means no new devices found
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncRepositoryImpl", "Error in connection check: ${e.message}", e)
                val error = syncErrorHandler.handleConnectionError(e)
                syncErrorHandler.logError(error)
                emit(emptyList())
            }
        }
    }
    
    override suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean {
        android.util.Log.d("SyncRepositoryImpl", "Starting connection to ${deviceInfo.name} (${deviceInfo.type})")
        android.util.Log.d("SyncRepositoryImpl", "Device info: hasSzopperApp=${deviceInfo.hasSzopperApp}, discoveryMethod=${deviceInfo.discoveryMethod}")
        _connectionStatus.value = ConnectionStatus.CONNECTING
        
        try {
            val connected = connectionRetryManager.executeWithRetryBoolean(
                config = ConnectionRetryManager.RetryConfig(maxRetries = 3)
            ) {
                android.util.Log.d("SyncRepositoryImpl", "Attempting WiFi Direct connection retry for ${deviceInfo.name}")
                val peerConnected = wifiDirectManager.connectToPeer(deviceInfo)
                if (peerConnected) {
                    android.util.Log.d("SyncRepositoryImpl", "Peer connected, requesting connection info")
                    val connectionInfo = wifiDirectManager.requestConnectionInfo()
                    if (connectionInfo != null) {
                        android.util.Log.d("SyncRepositoryImpl", "Connection info received, establishing data connection")
                        val dataConnected = dataTransferManager.establishConnection(deviceInfo, connectionInfo)
                        if (dataConnected) {
                            android.util.Log.d("SyncRepositoryImpl", "Data connection established, validating...")
                            // Validate the connection with ping/pong
                            dataTransferManager.validateConnection()
                        } else {
                            android.util.Log.e("SyncRepositoryImpl", "Failed to establish data connection")
                            false
                        }
                    } else {
                        android.util.Log.e("SyncRepositoryImpl", "Failed to get connection info")
                        false
                    }
                } else {
                    android.util.Log.e("SyncRepositoryImpl", "Failed to connect to WiFi Direct peer")
                    false
                }
            }
            
            if (connected) {
                android.util.Log.i("SyncRepositoryImpl", "✓ Successfully connected and validated connection to ${deviceInfo.name}")
                currentConnection = deviceInfo
                _connectionStatus.value = ConnectionStatus.CONNECTED
            } else {
                android.util.Log.e("SyncRepositoryImpl", "✗ Connection failed to ${deviceInfo.name}")
                _connectionStatus.value = ConnectionStatus.ERROR
            }
            
            return connected
        } catch (e: Exception) {
            android.util.Log.e("SyncRepositoryImpl", "Exception during connection to ${deviceInfo.name}: ${e.message}", e)
            val error = syncErrorHandler.handleConnectionError(e)
            syncErrorHandler.logError(error)
            _connectionStatus.value = ConnectionStatus.ERROR
            return false
        }
    }
    
    override suspend fun sendProducts(products: List<Product>): Boolean {
        if (!dataTransferManager.isConnected()) {
            return false
        }
        
        _connectionStatus.value = ConnectionStatus.SYNCING
        
        try {
            val success = connectionRetryManager.executeWithRetryBoolean(
                config = ConnectionRetryManager.RetryConfig(maxRetries = 2)
            ) {
                dataTransferManager.sendProducts(products)
            }
            
            _connectionStatus.value = if (success) ConnectionStatus.CONNECTED else ConnectionStatus.ERROR
            return success
        } catch (e: Exception) {
            val error = syncErrorHandler.handleDataTransferError(e)
            syncErrorHandler.logError(error)
            _connectionStatus.value = ConnectionStatus.ERROR
            return false
        }
    }
    
    override suspend fun receiveProducts(): List<Product> {
        if (!dataTransferManager.isConnected()) {
            return emptyList()
        }
        
        _connectionStatus.value = ConnectionStatus.SYNCING
        
        try {
            val serializableProducts = connectionRetryManager.executeWithRetry(
                config = ConnectionRetryManager.RetryConfig(maxRetries = 2)
            ) {
                dataTransferManager.receiveProducts()
            }
            
            val products = serializableProducts?.map { 
                dataTransferManager.serializableProductToProduct(it) 
            } ?: emptyList()
            
            _connectionStatus.value = ConnectionStatus.CONNECTED
            return products
        } catch (e: Exception) {
            val error = syncErrorHandler.handleDataTransferError(e)
            syncErrorHandler.logError(error)
            _connectionStatus.value = ConnectionStatus.ERROR
            return emptyList()
        }
    }
    
    suspend fun performFullSync(localProducts: List<Product>): List<Product>? {
        if (!dataTransferManager.isConnected()) {
            return null
        }
        
        _connectionStatus.value = ConnectionStatus.SYNCING
        
        try {
            val resolvedProducts = connectionRetryManager.executeWithRetry(
                config = ConnectionRetryManager.RetryConfig(maxRetries = 2)
            ) {
                dataTransferManager.performSync(localProducts)
            }
            
            val products = resolvedProducts?.map { 
                dataTransferManager.serializableProductToProduct(it) 
            }
            
            _connectionStatus.value = ConnectionStatus.CONNECTED
            return products
        } catch (e: Exception) {
            val error = syncErrorHandler.handleDataTransferError(e)
            syncErrorHandler.logError(error)
            _connectionStatus.value = ConnectionStatus.ERROR
            return null
        }
    }
    
    override suspend fun disconnect() {
        try {
            dataTransferManager.disconnect()
            wifiDirectManager.disconnect()
        } catch (e: Exception) {
            val error = syncErrorHandler.handleConnectionError(e)
            syncErrorHandler.logError(error)
        } finally {
            currentConnection = null
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }
    
    override fun getConnectionStatus(): Flow<ConnectionStatus> = _connectionStatus.asStateFlow()
}