package com.szopper.presentation.viewmodel

import com.szopper.domain.model.Product
import kotlinx.coroutines.flow.StateFlow
import org.mongodb.kbson.ObjectId

/**
 * Interface for ProductListViewModel to enable testing with fake implementations
 */
interface IProductListViewModel {
    val products: StateFlow<List<Product>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    fun addProduct(name: String)
    fun toggleProductBought(id: ObjectId)
    fun deleteProduct(product: Product)
    fun undoDelete(productId: ObjectId)
    fun resetAllProducts()
    fun reorderProducts(products: List<Product>)
}