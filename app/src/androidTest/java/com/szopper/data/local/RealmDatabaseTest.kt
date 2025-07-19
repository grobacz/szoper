package com.szopper.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.szopper.domain.model.Product
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RealmDatabaseTest {

    private lateinit var testRealm: Realm
    private lateinit var testConfig: RealmConfiguration

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        testConfig = RealmConfiguration.Builder(
            schema = setOf(Product::class)
        )
            .name("test_szopper.realm")
            .deleteRealmIfMigrationNeeded()
            .build()
            
        testRealm = Realm.open(testConfig)
        
        // Clear any existing data
        runTest {
            testRealm.write {
                deleteAll()
            }
        }
    }

    @After
    fun tearDown() {
        testRealm.close()
        Realm.deleteRealm(testConfig)
    }

    @Test
    fun testAddProduct() = runTest {
        // Given
        val productName = "Test Product"

        // When
        val addedProduct = testRealm.write {
            val product = Product().apply {
                name = productName
                isBought = false
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            copyToRealm(product)
        }

        // Then
        assertNotNull(addedProduct)
        assertEquals(productName, addedProduct.name)
        assertEquals(false, addedProduct.isBought)

        // Verify product is persisted
        val queriedProduct = testRealm.query<Product>("name == $0", productName).first().find()
        assertNotNull(queriedProduct)
        assertEquals(productName, queriedProduct.name)
    }

    @Test
    fun testUpdateProduct() = runTest {
        // Given - Add a product first
        val productName = "Test Product"
        val addedProduct = testRealm.write {
            val product = Product().apply {
                name = productName
                isBought = false
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            copyToRealm(product)
        }

        // When - Update the product
        testRealm.write {
            val productToUpdate = findLatest(addedProduct)
            productToUpdate?.let {
                it.isBought = true
                it.updatedAt = System.currentTimeMillis()
            }
        }

        // Then
        val updatedProduct = testRealm.query<Product>("name == $0", productName).first().find()
        assertNotNull(updatedProduct)
        assertTrue(updatedProduct.isBought)
    }

    @Test
    fun testDeleteProduct() = runTest {
        // Given - Add a product first
        val productName = "Test Product"
        val addedProduct = testRealm.write {
            val product = Product().apply {
                name = productName
                isBought = false
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            copyToRealm(product)
        }

        // When - Delete the product
        testRealm.write {
            val productToDelete = query<Product>("id == $0", addedProduct.id).first().find()
            productToDelete?.let { delete(it) }
        }

        // Then
        val deletedProduct = testRealm.query<Product>("name == $0", productName).first().find()
        assertEquals(null, deletedProduct)
    }

    @Test
    fun testQueryAllProducts() = runTest {
        // Given - Add multiple products
        val product1Name = "Product 1"
        val product2Name = "Product 2"

        testRealm.write {
            val product1 = Product().apply {
                name = product1Name
                isBought = false
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            val product2 = Product().apply {
                name = product2Name
                isBought = true
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            copyToRealm(product1)
            copyToRealm(product2)
        }

        // When
        val allProducts = testRealm.query<Product>().find()

        // Then
        assertEquals(2, allProducts.size)
        assertTrue(allProducts.any { it.name == product1Name })
        assertTrue(allProducts.any { it.name == product2Name })
    }

    @Test
    fun testResetAllProducts() = runTest {
        // Given - Add products with mixed bought status
        testRealm.write {
            val product1 = Product().apply {
                name = "Product 1"
                isBought = true
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            val product2 = Product().apply {
                name = "Product 2"
                isBought = true
                createdAt = System.currentTimeMillis()
                updatedAt = System.currentTimeMillis()
            }
            copyToRealm(product1)
            copyToRealm(product2)
        }

        // When - Reset all products
        testRealm.write {
            val products = query<Product>().find()
            products.forEach {
                it.isBought = false
                it.updatedAt = System.currentTimeMillis()
            }
        }

        // Then
        val allProducts = testRealm.query<Product>().find()
        assertEquals(2, allProducts.size)
        assertTrue(allProducts.all { !it.isBought })
    }
}