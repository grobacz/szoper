package com.szopper.data.sync

import com.szopper.domain.sync.ConflictStrategy
import com.szopper.domain.sync.MessageType
import com.szopper.domain.sync.SerializableProduct
import com.szopper.domain.sync.SyncData
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncProtocolHandlerTest {

    private lateinit var syncProtocolHandler: SyncProtocolHandler

    @Before
    fun setup() {
        syncProtocolHandler = SyncProtocolHandler()
    }

    @Test
    fun `createSyncRequest should create correct message`() {
        // Given
        val deviceId = "test-device"
        val timestamp = 12345L

        // When
        val message = syncProtocolHandler.createSyncRequest(deviceId, timestamp)

        // Then
        assertEquals(MessageType.SYNC_REQUEST, message.type)
        assertEquals(deviceId, message.deviceId)
        assertTrue(message.data is SyncData.SyncRequest)
        assertEquals(timestamp, (message.data as SyncData.SyncRequest).lastSyncTimestamp)
    }

    @Test
    fun `createProductListMessage should create correct message`() {
        // Given
        val deviceId = "test-device"
        val products = listOf(
            SerializableProduct("1", "Product 1", false, 0, 100L, 200L),
            SerializableProduct("2", "Product 2", true, 1, 150L, 250L)
        )

        // When
        val message = syncProtocolHandler.createProductListMessage(deviceId, products)

        // Then
        assertEquals(MessageType.PRODUCT_LIST, message.type)
        assertEquals(deviceId, message.deviceId)
        assertTrue(message.data is SyncData.ProductList)
        assertEquals(products, (message.data as SyncData.ProductList).products)
    }

    @Test
    fun `serializeMessage and deserializeMessage should work correctly`() {
        // Given
        val deviceId = "test-device"
        val products = listOf(
            SerializableProduct("1", "Product 1", false, 0, 100L, 200L)
        )
        val originalMessage = syncProtocolHandler.createProductListMessage(deviceId, products)

        // When
        val serialized = syncProtocolHandler.serializeMessage(originalMessage)
        val deserialized = syncProtocolHandler.deserializeMessage(serialized)

        // Then
        assertNotNull(deserialized)
        assertEquals(originalMessage.type, deserialized.type)
        assertEquals(originalMessage.deviceId, deserialized.deviceId)
        assertTrue(deserialized.data is SyncData.ProductList)
        assertEquals(products, (deserialized.data as SyncData.ProductList).products)
    }

    @Test
    fun `deserializeMessage should return null for invalid JSON`() {
        // Given
        val invalidJson = "invalid json string"

        // When
        val result = syncProtocolHandler.deserializeMessage(invalidJson)

        // Then
        assertNull(result)
    }

    @Test
    fun `resolveConflicts with LAST_UPDATED_WINS should prefer newer products`() {
        // Given
        val localProducts = listOf(
            SerializableProduct("1", "Product 1", false, 0, 100L, 200L),
            SerializableProduct("2", "Product 2", true, 1, 150L, 250L)
        )
        val remoteProducts = listOf(
            SerializableProduct("1", "Product 1 Updated", true, 0, 100L, 300L), // Newer
            SerializableProduct("3", "Product 3", false, 2, 180L, 280L) // New product
        )

        // When
        val resolution = syncProtocolHandler.resolveConflicts(
            localProducts, 
            remoteProducts, 
            ConflictStrategy.LAST_UPDATED_WINS
        )

        // Then
        assertEquals(ConflictStrategy.LAST_UPDATED_WINS, resolution.strategy)
        assertEquals(3, resolution.resolvedProducts.size)
        
        // Product 1 should be the remote version (newer)
        val product1 = resolution.resolvedProducts.find { it.id == "1" }
        assertNotNull(product1)
        assertEquals("Product 1 Updated", product1.name)
        assertEquals(true, product1.isBought)
        
        // Product 2 should be the local version (newer)
        val product2 = resolution.resolvedProducts.find { it.id == "2" }
        assertNotNull(product2)
        assertEquals("Product 2", product2.name)
        
        // Product 3 should be added from remote
        val product3 = resolution.resolvedProducts.find { it.id == "3" }
        assertNotNull(product3)
        assertEquals("Product 3", product3.name)
    }

    @Test
    fun `resolveConflicts with MERGE_ALL should combine bought status`() {
        // Given
        val localProducts = listOf(
            SerializableProduct("1", "Product 1", true, 0, 100L, 200L)
        )
        val remoteProducts = listOf(
            SerializableProduct("1", "Product 1 Updated", false, 0, 100L, 300L) // Newer but not bought
        )

        // When
        val resolution = syncProtocolHandler.resolveConflicts(
            localProducts, 
            remoteProducts, 
            ConflictStrategy.MERGE_ALL
        )

        // Then
        assertEquals(ConflictStrategy.MERGE_ALL, resolution.strategy)
        assertEquals(1, resolution.resolvedProducts.size)
        
        val product1 = resolution.resolvedProducts[0]
        assertEquals("Product 1 Updated", product1.name) // Should use newer name
        assertEquals(true, product1.isBought) // Should preserve bought status
    }

    @Test
    fun `validateMessage should return true for valid messages`() {
        // Given
        val validMessage = syncProtocolHandler.createSyncRequest("device", 123L)

        // When
        val isValid = syncProtocolHandler.validateMessage(validMessage)

        // Then
        assertTrue(isValid)
    }
}