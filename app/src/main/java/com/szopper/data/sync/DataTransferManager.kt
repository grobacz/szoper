package com.szopper.data.sync

import android.content.Context
import android.net.wifi.p2p.WifiP2pInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import com.szopper.data.sync.wifi.WifiDirectDataTransfer
import com.szopper.domain.model.Product
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.MessageType
import com.szopper.domain.sync.SerializableProduct
import com.szopper.domain.sync.SyncData
import com.szopper.domain.sync.SyncMessage
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
    private val syncProtocolHandler: SyncProtocolHandler,
    @ApplicationContext private val context: Context
) {
    
    private var currentConnection: Any? = null
    private var currentDeviceType: DeviceType? = null
    private var isServer: Boolean = false
    
    suspend fun establishConnection(
        deviceInfo: DeviceInfo,
        connectionInfo: Any? = null
    ): Boolean {
        android.util.Log.d("DataTransferManager", "Establishing WiFi Direct connection to ${deviceInfo.name}")
        val wifiInfo = connectionInfo as? WifiP2pInfo
        if (wifiInfo != null) {
            android.util.Log.d("DataTransferManager", "WiFi info available, establishing socket connection")
            val socket = wifiDirectDataTransfer.establishConnection(wifiInfo)
            if (socket != null) {
                android.util.Log.d("DataTransferManager", "Socket established, performing handshake")
                isServer = wifiInfo.isGroupOwner
                val handshakeSuccess = wifiDirectDataTransfer.performHandshake(socket, isServer)
                if (handshakeSuccess) {
                    android.util.Log.d("DataTransferManager", "Handshake successful for WiFi Direct")
                    currentConnection = socket
                    currentDeviceType = DeviceType.WIFI_DIRECT
                    return true
                } else {
                    android.util.Log.e("DataTransferManager", "Handshake failed for WiFi Direct")
                    socket.close()
                    return false
                }
            } else {
                return false
            }
        } else {
            return false
        }
    }
    
    suspend fun sendProducts(products: List<Product>): Boolean {
        val connection = currentConnection ?: return false
        
        try {
            val serializableProducts = products.map { it.toSerializable() }
            val message = syncProtocolHandler.createProductListMessage(
                deviceId = getDeviceId(),
                products = serializableProducts
            )
            val messageString = syncProtocolHandler.serializeMessage(message)
            
            return wifiDirectDataTransfer.sendData(connection as Socket, messageString)
        } catch (e: Exception) {
            return false
        }
    }
    
    suspend fun receiveProducts(): List<SerializableProduct>? {
        val connection = currentConnection ?: return null
        
        try {
            val messageString = wifiDirectDataTransfer.receiveData(connection as Socket)
            
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
                
                val sendSuccess = if (currentDeviceType == DeviceType.WIFI_DIRECT) {
                    wifiDirectDataTransfer.sendData(currentConnection as Socket, requestString)
                } else {
                    false
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
            wifiDirectDataTransfer.closeConnections()
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
            position = serializable.position
            createdAt = serializable.createdAt
            updatedAt = serializable.updatedAt
        }
    }

    
    suspend fun validateConnection(): Boolean {
        val connection = currentConnection ?: return false
        
        return try {
            android.util.Log.d("DataTransferManager", "Validating connection with ping/pong test")
            
            // Send ping message
            val pingMessage = syncProtocolHandler.createPingMessage(getDeviceId())
            val pingSuccess = if (currentDeviceType == DeviceType.WIFI_DIRECT) {
                wifiDirectDataTransfer.sendData(connection as Socket, 
                    syncProtocolHandler.serializeMessage(pingMessage))
            } else {
                false
            }
            
            if (!pingSuccess) {
                android.util.Log.e("DataTransferManager", "✗ Failed to send ping message")
                return false
            }
            
            // Wait for pong response with timeout
            withTimeout(5000) {
                val response = if (currentDeviceType == DeviceType.WIFI_DIRECT) {
                    wifiDirectDataTransfer.receiveData(connection as Socket)
                } else {
                    null
                }
                
                response?.let { responseStr ->
                    val message = syncProtocolHandler.deserializeMessage(responseStr)
                    if (message?.type == MessageType.PONG) {
                        android.util.Log.d("DataTransferManager", "✓ Connection validation successful")
                        true
                    } else {
                        android.util.Log.e("DataTransferManager", "✗ Invalid pong response")
                        false
                    }
                } ?: run {
                    android.util.Log.e("DataTransferManager", "✗ No pong response received")
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DataTransferManager", "✗ Connection validation failed: ${e.message}")
            false
        }
    }
    
    suspend fun handleIncomingPing(message: SyncMessage): Boolean {
        if (message.type != MessageType.PING) return false
        
        val connection = currentConnection ?: return false
        
        try {
            android.util.Log.d("DataTransferManager", "Responding to ping with pong")
            val pongMessage = syncProtocolHandler.createPongMessage(getDeviceId())
            
            return if (currentDeviceType == DeviceType.WIFI_DIRECT) {
                wifiDirectDataTransfer.sendData(connection as Socket, 
                    syncProtocolHandler.serializeMessage(pongMessage))
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DataTransferManager", "Failed to send pong response: ${e.message}")
            return false
        }
    }
}