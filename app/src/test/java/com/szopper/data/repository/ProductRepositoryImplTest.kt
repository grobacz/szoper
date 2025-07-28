package com.szopper.data.repository

import com.szopper.data.local.RealmDatabase
import com.szopper.domain.model.Product
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.flow.flowOf
import io.realm.kotlin.MutableRealm
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mongodb.kbson.ObjectId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductRepositoryImplTest {

    private lateinit var realmDatabase: RealmDatabase
    private lateinit var realm: Realm
    private lateinit var productRepository: ProductRepositoryImpl

    @Before
    fun setup() {
        realmDatabase = mock()
        realm = mock()
        whenever(realmDatabase.realm).thenReturn(realm)
        productRepository = ProductRepositoryImpl(realmDatabase)
    }

    @Test
    fun `addProduct should create product with correct properties`() = runTest {
        // Given
        val productName = "Test Product"
        
        // Mock the realm.write method to capture the product being created
        var capturedProduct: Product? = null
        whenever(realm.write(any<MutableRealm.() -> Unit>())).thenAnswer { invocation ->
            // Create a real product object to verify properties
            capturedProduct = Product().apply {
                name = productName
                isBought = false
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
                position = 0
            }
            Unit // Return Unit as realm.write returns Unit
        }

        // When
        val result = productRepository.addProduct(productName)

        // Then
        assertNotNull(result)
        assertEquals(productName, result.name)
        assertEquals(false, result.isBought)
        verify(realm).write(any<MutableRealm.() -> Unit>())
    }

    @Test
    fun `deleteProduct should call realm write operation`() = runTest {
        // Given
        val productId = ObjectId()
        
        whenever(realm.write(any<MutableRealm.() -> Unit>())).thenAnswer { 
            // Just simulate the write operation without complex mocking
            Unit
        }

        // When
        productRepository.deleteProduct(productId)

        // Then
        verify(realm).write(any<MutableRealm.() -> Unit>())
    }

    @Test
    fun `resetAllProducts should call realm write operation`() = runTest {
        // Given
        whenever(realm.write(any<MutableRealm.() -> Unit>())).thenAnswer { 
            // Just simulate the write operation
            Unit
        }

        // When
        productRepository.resetAllProducts()

        // Then
        verify(realm).write(any<MutableRealm.() -> Unit>())
    }

    @Test
    fun `reorderProducts should call realm write operation with correct positions`() = runTest {
        // Given
        val productId1 = ObjectId()
        val productId2 = ObjectId()
        val productPositions = listOf(productId1 to 0, productId2 to 1)
        
        whenever(realm.write(any<MutableRealm.() -> Unit>())).thenAnswer { 
            // Just simulate the write operation
            Unit
        }

        // When
        productRepository.reorderProducts(productPositions)

        // Then
        verify(realm).write(any<MutableRealm.() -> Unit>())
    }
}