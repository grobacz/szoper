package com.szopper.domain.usecase

import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = repository.getAllProducts()
}
