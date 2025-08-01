package com.szopper.data.repository

import com.szopper.data.local.RealmDatabase
import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val realmDatabase: RealmDatabase
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return realmDatabase.realm
            .query<Product>()
            .sort("position")
            .asFlow()
            .map { it.list.toList() }
    }

    override suspend fun addProduct(name: String): Product {
        val product = Product().apply {
            this.name = name
            this.isBought = false
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }
        
        realmDatabase.realm.write {
            val maxPosition = query<Product>().max("position", Int::class).find()
            product.position = (maxPosition ?: -1) + 1
            copyToRealm(product)
        }
        
        return product
    }

    override suspend fun updateProduct(product: Product) {
        realmDatabase.realm.write {
            val managedProduct = findLatest(product)
            managedProduct?.let {
                it.updatedAt = System.currentTimeMillis()
            }
        }
    }

    override suspend fun deleteProduct(id: ObjectId) {
        realmDatabase.realm.write {
            val product = query<Product>("id == $0", id).first().find()
            product?.let { productToDelete ->
                val deletedPosition = productToDelete.position
                
                // Delete the product
                delete(productToDelete)
                
                // Reindex positions for remaining products
                val remainingProducts = query<Product>("position > $0", deletedPosition)
                    .sort("position")
                    .find()
                
                remainingProducts.forEach { remainingProduct ->
                    remainingProduct.position = remainingProduct.position - 1
                    remainingProduct.updatedAt = System.currentTimeMillis()
                }
            }
        }
    }

    override suspend fun toggleProductBought(id: ObjectId) {
        realmDatabase.realm.write {
            val product = query<Product>("id == $0", id).first().find()
            product?.let {
                it.isBought = !it.isBought
                it.updatedAt = System.currentTimeMillis()
            }
        }
    }

    override fun getProductsPaginated(limit: Int, offset: Int): Flow<List<Product>> {
        return realmDatabase.realm
            .query<Product>()
            .sort("position")
            .limit(limit)
            .asFlow()
            .map { results ->
                results.list.drop(offset).take(limit).toList()
            }
    }

    override suspend fun getProductCount(): Long {
        return realmDatabase.realm.query<Product>().count().find()
    }

    override suspend fun resetAllProducts() {
        realmDatabase.realm.write {
            val products = query<Product>().find()
            products.forEach {
                it.isBought = false
                it.updatedAt = System.currentTimeMillis()
            }
        }
    }

    override suspend fun reorderProducts(productPositions: List<Pair<ObjectId, Int>>) {
        realmDatabase.realm.write {
            productPositions.forEach { (id, position) ->
                val product = query<Product>("id == $0", id).first().find()
                product?.let {
                    it.position = position
                    it.updatedAt = System.currentTimeMillis()
                }
            }
        }
    }
}