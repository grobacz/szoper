package com.szopper.data.sync

import com.szopper.data.sync.bluetooth.BluetoothManager
import com.szopper.data.sync.wifi.WifiDirectManager
import com.szopper.domain.model.Product
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val wifiDirectManager: WifiDirectManager,
    private val bluetoothManager: BluetoothManager,
    private val dataTransferManager: DataTransferManager,
    private val connectionRetryManager: ConnectionRetryManager,
    private val syncErrorHandler: SyncErrorHandler
) : SyncRepository {
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private var currentConnection: DeviceInfo? = null
    
    override fun discoverDevices(): Flow<List<DeviceInfo>> {
        _connectionStatus.value = ConnectionStatus.DISCOVERING
        
        return combine(
            wifiDirectManager.discoverPeers(),
            bluetoothManager.discoverDevices()
        ) { wifiDevices, bluetoothDevices ->
            (wifiDevices + bluetoothDevices).distinctBy { it.id }
        }
    }
    
    override suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        
        try {
            val connected = connectionRetryManager.executeWithRetryBoolean(
                config = ConnectionRetryManager.RetryConfig(maxRetries = 3)
            ) {
                when (deviceInfo.type) {
                    DeviceType.WIFI_DIRECT -> {
                        val connectionInfo = wifiDirectManager.requestConnectionInfo()
                        if (connectionInfo != null) {
                            dataTransferManager.establishConnection(deviceInfo, connectionInfo)
                        } else {
                            false
                        }
                    }
                    DeviceType.BLUETOOTH -> {
                        dataTransferManager.establishConnection(deviceInfo)
                    }
                }
            }
            
            if (connected) {
                currentConnection = deviceInfo
                _connectionStatus.value = ConnectionStatus.CONNECTED
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
            
            return connected
        } catch (e: Exception) {
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
            currentConnection?.let { device ->
                when (device.type) {
                    DeviceType.WIFI_DIRECT -> wifiDirectManager.disconnect()
                    DeviceType.BLUETOOTH -> {
                        // Additional Bluetooth cleanup if needed
                    }
                }
            }
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