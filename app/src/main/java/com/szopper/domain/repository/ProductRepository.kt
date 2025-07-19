package com.szopper.domain.repository

import com.szopper.domain.model.Product
import kotlinx.coroutines.flow.Flow
import org.mongodb.kbson.ObjectId

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getProductsPaginated(limit: Int, offset: Int): Flow<List<Product>>
    suspend fun addProduct(name: String): Product
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(id: ObjectId)
    suspend fun toggleProductBought(id: ObjectId)
    suspend fun resetAllProducts()
    suspend fun getProductCount(): Long
}