package com.szopper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szopper.domain.model.Product
import com.szopper.domain.usecase.AddProductUseCase
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
    private val reorderProductsUseCase: ReorderProductsUseCase
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
}