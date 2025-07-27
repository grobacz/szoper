package com.szopper.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.szopper.domain.model.Product
import com.szopper.domain.sync.ConflictStrategy
import com.szopper.domain.sync.SerializableProduct
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {

    private lateinit var syncProtocolHandler: SyncProtocolHandler
    private lateinit var dataTransferManager: DataTransferManager
    private lateinit var connectionRetryManager: ConnectionRetryManager

    @Before
    fun setup() {
        syncProtocolHandler = SyncProtocolHandler()
        connectionRetryManager = ConnectionRetryManager()
        // Note: DataTransferManager requires actual managers which need Android context
        // For this test, we'll focus on protocol handling
    }

    @Test
    fun testCompleteMessageSerializationFlow() = runTest {
        // Given
        val deviceId = "test-device-123"
        val products = listOf(
            SerializableProduct("1", "Milk", false, 0, 1000L, 1100L),
            SerializableProduct("2", "Bread", true, 1, 1050L, 1150L),
            SerializableProduct("3", "Eggs", false, 2, 1200L, 1200L)
        )

        // When - Create and serialize message
        val originalMessage = syncProtocolHandler.createProductListMessage(deviceId, products)
        val serializedMessage = syncProtocolHandler.serializeMessage(originalMessage)
        val deserializedMessage = syncProtocolHandler.deserializeMessage(serializedMessage)

        // Then
        assertNotNull(deserializedMessage)
        assertEquals(originalMessage.type, deserializedMessage.type)
        assertEquals(originalMessage.deviceId, deserializedMessage.deviceId)
        
        val originalProducts = (originalMessage.data as com.szopper.domain.sync.SyncData.ProductList).products
        val deserializedProducts = (deserializedMessage.data as com.szopper.domain.sync.SyncData.ProductList).products
        
        assertEquals(originalProducts.size, deserializedProducts.size)
        assertEquals(originalProducts, deserializedProducts)
    }

    @Test
    fun testConflictResolutionWithRealData() = runTest {
        // Given - Simulate two devices with overlapping shopping lists
        val device1Products = listOf(
            SerializableProduct("1", "Milk", true, 1000L, 1500L),   // Bought on device 1, updated later
            SerializableProduct("2", "Bread", false, 1100L, 1200L), // Not bought on device 1
            SerializableProduct("3", "Eggs", false, 1200L, 1200L)   // Only on device 1
        )
        
        val device2Products = listOf(
            SerializableProduct("1", "Milk", false, 1000L, 1300L),  // Not bought on device 2, updated earlier
            SerializableProduct("2", "Bread", true, 1100L, 1400L),  // Bought on device 2, updated later
            SerializableProduct("4", "Butter", false, 1250L, 1250L) // Only on device 2
        )

        // When - Resolve conflicts using LAST_UPDATED_WINS
        val resolution = syncProtocolHandler.resolveConflicts(
            device1Products,
            device2Products,
            ConflictStrategy.LAST_UPDATED_WINS
        )

        // Then
        assertEquals(4, resolution.resolvedProducts.size)
        
        // Milk should use device 1 version (more recent update)
        val milk = resolution.resolvedProducts.find { it.id == "1" }
        assertNotNull(milk)
        assertEquals("Milk", milk.name)
        assertTrue(milk.isBought) // Device 1 had it bought and was more recent
        assertEquals(1500L, milk.updatedAt)
        
        // Bread should use device 2 version (more recent update)
        val bread = resolution.resolvedProducts.find { it.id == "2" }
        assertNotNull(bread)
        assertEquals("Bread", bread.name)
        assertTrue(bread.isBought) // Device 2 had it bought and was more recent
        assertEquals(1400L, bread.updatedAt)
        
        // Eggs should be included (only on device 1)
        val eggs = resolution.resolvedProducts.find { it.id == "3" }
        assertNotNull(eggs)
        assertEquals("Eggs", eggs.name)
        
        // Butter should be included (only on device 2)
        val butter = resolution.resolvedProducts.find { it.id == "4" }
        assertNotNull(butter)
        assertEquals("Butter", butter.name)
    }

    @Test
    fun testConflictResolutionWithMergeStrategy() = runTest {
        // Given - Same product with different bought status
        val device1Products = listOf(
            SerializableProduct("1", "Milk", true, 1000L, 1200L)  // Bought on device 1
        )
        
        val device2Products = listOf(
            SerializableProduct("1", "Organic Milk", false, 1000L, 1400L)  // Not bought on device 2, but renamed and newer
        )

        // When - Resolve conflicts using MERGE_ALL
        val resolution = syncProtocolHandler.resolveConflicts(
            device1Products,
            device2Products,
            ConflictStrategy.MERGE_ALL
        )

        // Then
        assertEquals(1, resolution.resolvedProducts.size)
        
        val milk = resolution.resolvedProducts[0]
        assertEquals("Organic Milk", milk.name) // Should use newer name from device 2
        assertTrue(milk.isBought) // Should preserve bought status from either device
        assertEquals(1400L, milk.updatedAt) // Should use newer timestamp
    }

    @Test
    fun testRetryMechanismSimulation() = runTest {
        // Given
        var attemptCount = 0
        val maxRetries = 3
        
        // When - Simulate connection attempts that fail initially
        val result = connectionRetryManager.executeWithRetryBoolean(
            ConnectionRetryManager.RetryConfig(maxRetries = maxRetries, initialDelayMs = 10)
        ) {
            attemptCount++
            // Simulate failure on first two attempts, success on third
            attemptCount >= 3
        }

        // Then
        assertTrue(result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun testMessageValidation() = runTest {
        // Given
        val validSyncRequest = syncProtocolHandler.createSyncRequest("device1", 1000L)
        val validProductList = syncProtocolHandler.createProductListMessage(
            "device2", 
            listOf(SerializableProduct("1", "Test", false, 100L, 200L))
        )
        val validHeartbeat = syncProtocolHandler.createHeartbeat("device3", "Test Device")

        // When & Then
        assertTrue(syncProtocolHandler.validateMessage(validSyncRequest))
        assertTrue(syncProtocolHandler.validateMessage(validProductList))
        assertTrue(syncProtocolHandler.validateMessage(validHeartbeat))
    }
}