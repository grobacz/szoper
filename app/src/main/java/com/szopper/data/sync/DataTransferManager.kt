package com.szopper.data.sync

import android.bluetooth.BluetoothSocket
import android.net.wifi.p2p.WifiP2pInfo
import com.szopper.data.sync.bluetooth.BluetoothDataTransfer
import com.szopper.data.sync.wifi.WifiDirectDataTransfer
import com.szopper.domain.model.Product
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.MessageType
import com.szopper.domain.sync.SerializableProduct
import com.szopper.domain.sync.SyncData
import com.szopper.domain.sync.toSerializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.mongodb.kbson.ObjectId
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataTransferManager @Inject constructor(
    private val wifiDirectDataTransfer: WifiDirectDataTransfer,
    private val bluetoothDataTransfer: BluetoothDataTransfer,
    private val syncProtocolHandler: SyncProtocolHandler
) {
    
    private var currentConnection: Any? = null
    private var currentDeviceType: DeviceType? = null
    private var isServer: Boolean = false
    
    suspend fun establishConnection(
        deviceInfo: DeviceInfo,
        connectionInfo: Any? = null
    ): Boolean {
        return when (deviceInfo.type) {
            DeviceType.WIFI_DIRECT -> {
                val wifiInfo = connectionInfo as? WifiP2pInfo
                if (wifiInfo != null) {
                    val socket = wifiDirectDataTransfer.establishConnection(wifiInfo)
                    if (socket != null) {
                        isServer = wifiInfo.isGroupOwner
                        val handshakeSuccess = wifiDirectDataTransfer.performHandshake(socket, isServer)
                        if (handshakeSuccess) {
                            currentConnection = socket
                            currentDeviceType = DeviceType.WIFI_DIRECT
                            true
                        } else {
                            socket.close()
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
            DeviceType.BLUETOOTH -> {
                // For Bluetooth, we need to determine if we should be server or client
                // For simplicity, device with lexicographically smaller address becomes server
                val shouldBeServer = deviceInfo.id < android.provider.Settings.Secure.getString(
                    null, // This would need proper context injection
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: ""
                
                val socket = if (shouldBeServer) {
                    bluetoothDataTransfer.startServer()
                    bluetoothDataTransfer.waitForClient()
                } else {
                    if (bluetoothDataTransfer.connectToDevice(deviceInfo.id)) {
                        bluetoothDataTransfer.getCurrentClientSocket()
                    } else {
                        null
                    }
                }
                
                if (socket != null) {
                    isServer = shouldBeServer
                    val handshakeSuccess = bluetoothDataTransfer.performHandshake(socket, isServer)
                    if (handshakeSuccess) {
                        currentConnection = socket
                        currentDeviceType = DeviceType.BLUETOOTH
                        true
                    } else {
                        socket.close()
                        false
                    }
                } else {
                    false
                }
            }
        }
    }
    
    suspend fun sendProducts(products: List<Product>): Boolean {
        val connection = currentConnection ?: return false
        val deviceType = currentDeviceType ?: return false
        
        try {
            val serializableProducts = products.map { it.toSerializable() }
            val message = syncProtocolHandler.createProductListMessage(
                deviceId = getDeviceId(),
                products = serializableProducts
            )
            val messageString = syncProtocolHandler.serializeMessage(message)
            
            return when (deviceType) {
                DeviceType.WIFI_DIRECT -> {
                    wifiDirectDataTransfer.sendData(connection as Socket, messageString)
                }
                DeviceType.BLUETOOTH -> {
                    bluetoothDataTransfer.sendData(connection as BluetoothSocket, messageString)
                }
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    suspend fun receiveProducts(): List<SerializableProduct>? {
        val connection = currentConnection ?: return null
        val deviceType = currentDeviceType ?: return null
        
        try {
            val messageString = when (deviceType) {
                DeviceType.WIFI_DIRECT -> {
                    wifiDirectDataTransfer.receiveData(connection as Socket)
                }
                DeviceType.BLUETOOTH -> {
                    bluetoothDataTransfer.receiveData(connection as BluetoothSocket)
                }
            }
            
            messageString?.let { msg ->
                val message = syncProtocolHandler.deserializeMessage(msg)
                if (message?.type == MessageType.PRODUCT_LIST) {
                    val productListData = message.data as? SyncData.ProductList
                    return productListData?.products
                }
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    suspend fun performSync(localProducts: List<Product>): List<SerializableProduct>? {
        return withTimeout(30000) { // 30 second timeout
            try {
                // Send sync request
                val syncRequest = syncProtocolHandler.createSyncRequest(
                    deviceId = getDeviceId(),
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                val requestString = syncProtocolHandler.serializeMessage(syncRequest)
                
                val sendSuccess = when (currentDeviceType) {
                    DeviceType.WIFI_DIRECT -> {
                        wifiDirectDataTransfer.sendData(currentConnection as Socket, requestString)
                    }
                    DeviceType.BLUETOOTH -> {
                        bluetoothDataTransfer.sendData(currentConnection as BluetoothSocket, requestString)
                    }
                    null -> false
                }
                
                if (!sendSuccess) return@withTimeout null
                
                // Wait for sync response
                delay(1000) // Give remote device time to process
                
                // Send our products
                if (!sendProducts(localProducts)) return@withTimeout null
                
                // Receive remote products
                val remoteProducts = receiveProducts()
                
                // Perform conflict resolution
                if (remoteProducts != null) {
                    val localSerializable = localProducts.map { it.toSerializable() }
                    val resolution = syncProtocolHandler.resolveConflicts(
                        localProducts = localSerializable,
                        remoteProducts = remoteProducts
                    )
                    resolution.resolvedProducts
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun disconnect() {
        currentConnection?.let {
            when (currentDeviceType) {
                DeviceType.WIFI_DIRECT -> {
                    wifiDirectDataTransfer.closeConnections()
                }
                DeviceType.BLUETOOTH -> {
                    bluetoothDataTransfer.closeConnections()
                }
                null -> {}
            }
        }
        currentConnection = null
        currentDeviceType = null
        isServer = false
    }
    
    fun isConnected(): Boolean = currentConnection != null
    
    private fun getDeviceId(): String {
        // This would typically use the actual device ID
        return "device_${System.currentTimeMillis()}"
    }
    
    fun serializableProductToProduct(serializable: SerializableProduct): Product {
        return Product().apply {
            id = ObjectId(serializable.id)
            name = serializable.name
            isBought = serializable.isBought
            createdAt = serializable.createdAt
            updatedAt = serializable.updatedAt
        }
    }
}