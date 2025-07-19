package com.szopper.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.szopper.domain.model.Product
import com.szopper.domain.usecase.AddProductUseCase
import com.szopper.domain.usecase.GetProductsPaginatedUseCase
import com.szopper.domain.usecase.ResetAllProductsUseCase
import com.szopper.domain.usecase.ToggleProductBoughtUseCase
import com.szopper.presentation.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

@HiltViewModel
class PaginatedProductListViewModel @Inject constructor(
    private val getProductsPaginatedUseCase: GetProductsPaginatedUseCase,
    private val addProductUseCase: AddProductUseCase,
    private val toggleProductBoughtUseCase: ToggleProductBoughtUseCase,
    private val resetAllProductsUseCase: ResetAllProductsUseCase,
    application: Application
) : AndroidViewModel(application) {
    
    private val memoryManager = MemoryManager(application)

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasMoreItems = MutableStateFlow(true)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

    private var currentPage = 0
    private val pageSize: Int
        get() = memoryManager.getOptimalPageSize()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            getProductsPaginatedUseCase(pageSize, 0)
                .catch { throwable ->
                    _error.value = throwable.message
                    _isLoading.value = false
                }
                .collect { productList ->
                    _products.value = productList
                    _isLoading.value = false
                    _hasMoreItems.value = productList.size == pageSize
                    _error.value = null
                    currentPage = 1
                }
        }
    }

    fun loadMoreProducts() {
        if (_isLoadingMore.value || !_hasMoreItems.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            getProductsPaginatedUseCase(pageSize, currentPage * pageSize)
                .catch { throwable ->
                    _error.value = throwable.message
                    _isLoadingMore.value = false
                }
                .collect { newProducts ->
                    val currentProducts = _products.value.toMutableList()
                    currentProducts.addAll(newProducts)
                    _products.value = currentProducts
                    _isLoadingMore.value = false
                    _hasMoreItems.value = newProducts.size == pageSize
                    currentPage++
                }
        }
    }

    fun addProduct(name: String) {
        viewModelScope.launch {
            try {
                addProductUseCase(name)
                // Refresh the list to include the new product
                refreshProducts()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun toggleProductBought(id: ObjectId) {
        viewModelScope.launch {
            try {
                toggleProductBoughtUseCase(id)
                // Update the local list optimistically
                val updatedProducts = _products.value.map { product ->
                    if (product.id == id) {
                        product.apply { 
                            isBought = !isBought
                            updatedAt = System.currentTimeMillis()
                        }
                    } else {
                        product
                    }
                }
                _products.value = updatedProducts
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun resetAllProducts() {
        viewModelScope.launch {
            try {
                resetAllProductsUseCase()
                refreshProducts()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun refreshProducts() {
        currentPage = 0
        _hasMoreItems.value = true
        loadProducts()
    }

    fun clearError() {
        _error.value = null
    }
}