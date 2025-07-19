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
            .sort("createdAt", io.realm.kotlin.query.Sort.DESCENDING)
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
            product?.let { delete(it) }
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
            .sort("createdAt", io.realm.kotlin.query.Sort.DESCENDING)
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
}