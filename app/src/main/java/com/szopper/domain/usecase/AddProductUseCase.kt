package com.szopper.domain.usecase

import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import javax.inject.Inject

class AddProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(name: String): Product {
        require(name.isNotBlank()) { "Product name cannot be blank" }
        return repository.addProduct(name.trim())
    }
}
