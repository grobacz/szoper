package com.szopper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szopper.domain.model.Product
import com.szopper.domain.usecase.AddProductUseCase
import com.szopper.domain.usecase.DeleteProductUseCase
import com.szopper.domain.usecase.GetAllProductsUseCase
import com.szopper.domain.usecase.ReorderProductsUseCase
import com.szopper.domain.usecase.ResetAllProductsUseCase
import com.szopper.domain.usecase.ToggleProductBoughtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val addProductUseCase: AddProductUseCase,
    private val toggleProductBoughtUseCase: ToggleProductBoughtUseCase,
    private val resetAllProductsUseCase: ResetAllProductsUseCase,
    private val reorderProductsUseCase: ReorderProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Undo functionality - store recently deleted items
    private val _recentlyDeleted = MutableStateFlow<Map<ObjectId, Product>>(emptyMap())
    private val undoTimeoutMs = 5000L // 5 seconds

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            getAllProductsUseCase()
                .catch { throwable ->
                    _error.value = throwable.message
                    _isLoading.value = false
                }
                .collect { productList ->
                    _products.value = productList
                    _isLoading.value = false
                    _error.value = null
                }
        }
    }

    fun addProduct(name: String) {
        viewModelScope.launch {
            try {
                addProductUseCase(name)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun toggleProductBought(id: ObjectId) {
        viewModelScope.launch {
            try {
                toggleProductBoughtUseCase(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun resetAllProducts() {
        viewModelScope.launch {
            try {
                resetAllProductsUseCase()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun reorderProducts(products: List<Product>) {
        viewModelScope.launch {
            try {
                val productPositions = products.mapIndexed { index, product ->
                    product.id to index
                }
                reorderProductsUseCase(productPositions)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Deletes a product and stores it temporarily for undo functionality
     */
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                // Store product for undo functionality
                val currentDeleted = _recentlyDeleted.value.toMutableMap()
                currentDeleted[product.id] = product
                _recentlyDeleted.value = currentDeleted
                
                // Delete from database
                deleteProductUseCase(product.id)
                
                // Schedule removal from undo cache
                scheduleUndoCacheCleanup(product.id)
            } catch (e: Exception) {
                // Remove from undo cache if deletion failed
                val currentDeleted = _recentlyDeleted.value.toMutableMap()
                currentDeleted.remove(product.id)
                _recentlyDeleted.value = currentDeleted
                
                _error.value = e.message
            }
        }
    }

    /**
     * Restores a recently deleted product
     */
    fun undoDelete(productId: ObjectId) {
        viewModelScope.launch {
            try {
                val deletedProduct = _recentlyDeleted.value[productId]
                if (deletedProduct != null) {
                    // Re-add the product
                    addProductUseCase(deletedProduct.name)
                    
                    // Remove from undo cache
                    val currentDeleted = _recentlyDeleted.value.toMutableMap()
                    currentDeleted.remove(productId)
                    _recentlyDeleted.value = currentDeleted
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Schedules cleanup of undo cache after timeout
     */
    private fun scheduleUndoCacheCleanup(productId: ObjectId) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(undoTimeoutMs)
            
            // Remove from undo cache after timeout
            val currentDeleted = _recentlyDeleted.value.toMutableMap()
            currentDeleted.remove(productId)
            _recentlyDeleted.value = currentDeleted
        }
    }
}