package com.szopper.presentation.ui

import androidx.lifecycle.ViewModel
import com.szopper.domain.model.Product
import com.szopper.presentation.viewmodel.IProductListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mongodb.kbson.ObjectId

/**
 * Fake ViewModel implementation for testing UI components
 */
class FakeProductListViewModel : ViewModel(), IProductListViewModel {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    override val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    fun setProducts(products: List<Product>) {
        _products.value = products
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setError(error: String?) {
        _error.value = error
    }

    // Mock implementations of ViewModel methods that don't need to work in tests
    override fun addProduct(name: String) {
        // No-op for tests
    }

    override fun toggleProductBought(id: ObjectId) {
        // No-op for tests
    }

    override fun deleteProduct(product: Product) {
        // No-op for tests
    }

    override fun undoDelete(productId: ObjectId) {
        // No-op for tests
    }

    override fun resetAllProducts() {
        // No-op for tests
    }

    override fun reorderProducts(products: List<Product>) {
        // No-op for tests
    }
}