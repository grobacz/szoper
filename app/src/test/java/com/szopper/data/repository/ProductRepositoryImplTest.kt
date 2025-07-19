package com.szopper.data.repository

import com.szopper.data.local.RealmDatabase
import com.szopper.domain.model.Product
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.flow.flowOf
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
        val mockWriteTransaction = mock<Realm.WriteTransaction>()
        whenever(realm.write(any<suspend Realm.WriteTransaction.() -> Unit>())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend Realm.WriteTransaction.() -> Unit>(0)
            block.invoke(mockWriteTransaction)
        }

        // When
        val result = productRepository.addProduct(productName)

        // Then
        assertNotNull(result)
        assertEquals(productName, result.name)
        assertEquals(false, result.isBought)
    }

    @Test
    fun `deleteProduct should call realm delete with correct product`() = runTest {
        // Given
        val productId = ObjectId()
        val mockProduct = Product()
        val mockQuery = mock<RealmResults<Product>>()
        val mockWriteTransaction = mock<Realm.WriteTransaction>()
        
        whenever(realm.write(any<suspend Realm.WriteTransaction.() -> Unit>())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend Realm.WriteTransaction.() -> Unit>(0)
            block.invoke(mockWriteTransaction)
        }
        
        whenever(mockWriteTransaction.query<Product>(any<String>(), any())).thenReturn(mock())
        whenever(mockWriteTransaction.query<Product>(any<String>(), any()).first()).thenReturn(mock())
        whenever(mockWriteTransaction.query<Product>(any<String>(), any()).first().find()).thenReturn(mockProduct)

        // When
        productRepository.deleteProduct(productId)

        // Then
        verify(realm).write(any<suspend Realm.WriteTransaction.() -> Unit>())
    }

    @Test
    fun `resetAllProducts should mark all products as not bought`() = runTest {
        // Given
        val mockWriteTransaction = mock<Realm.WriteTransaction>()
        val mockResults = mock<RealmResults<Product>>()
        val product1 = Product().apply { isBought = true }
        val product2 = Product().apply { isBought = true }
        val products = listOf(product1, product2)
        
        whenever(realm.write(any<suspend Realm.WriteTransaction.() -> Unit>())).thenAnswer { invocation ->
            val block = invocation.getArgument<suspend Realm.WriteTransaction.() -> Unit>(0)
            block.invoke(mockWriteTransaction)
        }
        
        whenever(mockWriteTransaction.query<Product>()).thenReturn(mock())
        whenever(mockWriteTransaction.query<Product>().find()).thenReturn(mockResults)
        whenever(mockResults.iterator()).thenReturn(products.iterator())

        // When
        productRepository.resetAllProducts()

        // Then
        verify(realm).write(any<suspend Realm.WriteTransaction.() -> Unit>())
    }
}