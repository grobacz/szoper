package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import javax.inject.Inject

class ResetAllProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke() {
        repository.resetAllProducts()
    }
}
