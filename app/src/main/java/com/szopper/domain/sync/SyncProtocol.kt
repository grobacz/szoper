package com.szopper.domain.sync

import com.szopper.domain.model.Product
import kotlinx.serialization.Serializable

@Serializable
data class SyncMessage(
    val type: MessageType,
    val deviceId: String,
    val timestamp: Long,
    val data: SyncData? = null
)

@Serializable
sealed class SyncData {
    @Serializable
    data class ProductList(val products: List<SerializableProduct>) : SyncData()
    
    @Serializable
    data class SyncRequest(val lastSyncTimestamp: Long) : SyncData()
    
    @Serializable
    data class SyncResponse(val accepted: Boolean, val message: String = "") : SyncData()
    
    @Serializable
    data class Heartbeat(val deviceName: String) : SyncData()
}

enum class MessageType {
    SYNC_REQUEST,
    SYNC_RESPONSE,
    PRODUCT_LIST,
    HEARTBEAT,
    DISCONNECT
}

@Serializable
data class SerializableProduct(
    val id: String,
    val name: String,
    val isBought: Boolean,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long
)

fun Product.toSerializable(): SerializableProduct {
    return SerializableProduct(
        id = this.id.toHexString(),
        name = this.name,
        isBought = this.isBought,
        position = this.position,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

data class ConflictResolution(
    val strategy: ConflictStrategy,
    val resolvedProducts: List<SerializableProduct>
)

enum class ConflictStrategy {
    LAST_UPDATED_WINS,
    MERGE_ALL,
    MANUAL_RESOLUTION
}
