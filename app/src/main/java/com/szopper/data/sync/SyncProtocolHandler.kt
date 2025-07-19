package com.szopper.data.sync

import com.szopper.domain.sync.SyncMessage
import com.szopper.domain.sync.SyncData
import com.szopper.domain.sync.MessageType
import com.szopper.domain.sync.SerializableProduct
import com.szopper.domain.sync.ConflictResolution
import com.szopper.domain.sync.ConflictStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncProtocolHandler @Inject constructor() {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    fun serializeMessage(message: SyncMessage): String {
        return json.encodeToString(message)
    }
    
    fun deserializeMessage(messageString: String): SyncMessage? {
        return try {
            json.decodeFromString<SyncMessage>(messageString)
        } catch (e: Exception) {
            null
        }
    }
    
    fun createSyncRequest(deviceId: String, lastSyncTimestamp: Long): SyncMessage {
        return SyncMessage(
            type = MessageType.SYNC_REQUEST,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            data = SyncData.SyncRequest(lastSyncTimestamp)
        )
    }
    
    fun createProductListMessage(deviceId: String, products: List<SerializableProduct>): SyncMessage {
        return SyncMessage(
            type = MessageType.PRODUCT_LIST,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            data = SyncData.ProductList(products)
        )
    }
    
    fun createSyncResponse(deviceId: String, accepted: Boolean, message: String = ""): SyncMessage {
        return SyncMessage(
            type = MessageType.SYNC_RESPONSE,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            data = SyncData.SyncResponse(accepted, message)
        )
    }
    
    fun createHeartbeat(deviceId: String, deviceName: String): SyncMessage {
        return SyncMessage(
            type = MessageType.HEARTBEAT,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            data = SyncData.Heartbeat(deviceName)
        )
    }
    
    fun resolveConflicts(
        localProducts: List<SerializableProduct>,
        remoteProducts: List<SerializableProduct>,
        strategy: ConflictStrategy = ConflictStrategy.LAST_UPDATED_WINS
    ): ConflictResolution {
        
        val allProductsMap = mutableMapOf<String, SerializableProduct>()
        
        // Add all local products first
        localProducts.forEach { product ->
            allProductsMap[product.id] = product
        }
        
        // Process remote products based on strategy
        remoteProducts.forEach { remoteProduct ->
            val localProduct = allProductsMap[remoteProduct.id]
            
            when (strategy) {
                ConflictStrategy.LAST_UPDATED_WINS -> {
                    if (localProduct == null || remoteProduct.updatedAt > localProduct.updatedAt) {
                        allProductsMap[remoteProduct.id] = remoteProduct
                    }
                }
                ConflictStrategy.MERGE_ALL -> {
                    if (localProduct == null) {
                        allProductsMap[remoteProduct.id] = remoteProduct
                    } else {
                        // For merge, we keep the product that was updated more recently
                        // but preserve the "bought" status if either is bought
                        val mergedProduct = if (remoteProduct.updatedAt > localProduct.updatedAt) {
                            remoteProduct.copy(isBought = localProduct.isBought || remoteProduct.isBought)
                        } else {
                            localProduct.copy(isBought = localProduct.isBought || remoteProduct.isBought)
                        }
                        allProductsMap[remoteProduct.id] = mergedProduct
                    }
                }
                ConflictStrategy.MANUAL_RESOLUTION -> {
                    // For manual resolution, we would need UI intervention
                    // For now, default to last updated wins
                    if (localProduct == null || remoteProduct.updatedAt > localProduct.updatedAt) {
                        allProductsMap[remoteProduct.id] = remoteProduct
                    }
                }
            }
        }
        
        return ConflictResolution(
            strategy = strategy,
            resolvedProducts = allProductsMap.values.toList()
        )
    }
    
    fun validateMessage(message: SyncMessage): Boolean {
        return when (message.type) {
            MessageType.SYNC_REQUEST -> message.data is SyncData.SyncRequest
            MessageType.SYNC_RESPONSE -> message.data is SyncData.SyncResponse
            MessageType.PRODUCT_LIST -> message.data is SyncData.ProductList
            MessageType.HEARTBEAT -> message.data is SyncData.Heartbeat
            MessageType.DISCONNECT -> true
        }
    }
}