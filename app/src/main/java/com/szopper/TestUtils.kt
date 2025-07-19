package com.szopper

import com.szopper.domain.model.Product
import com.szopper.domain.sync.SerializableProduct
import org.mongodb.kbson.ObjectId

object TestUtils {
    
    fun createTestProduct(
        name: String = "Test Product",
        isBought: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Product {
        return Product().apply {
            this.name = name
            this.isBought = isBought
            this.createdAt = createdAt
            this.updatedAt = updatedAt
        }
    }
    
    fun createTestSerializableProduct(
        id: String = ObjectId().toHexString(),
        name: String = "Test Product",
        isBought: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): SerializableProduct {
        return SerializableProduct(
            id = id,
            name = name,
            isBought = isBought,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    fun createTestProductList(count: Int): List<Product> {
        return (1..count).map { index ->
            createTestProduct(
                name = "Product $index",
                isBought = index % 2 == 0 // Every other product is bought
            )
        }
    }
    
    fun createTestSerializableProductList(count: Int): List<SerializableProduct> {
        return (1..count).map { index ->
            createTestSerializableProduct(
                name = "Product $index",
                isBought = index % 2 == 0 // Every other product is bought
            )
        }
    }
}