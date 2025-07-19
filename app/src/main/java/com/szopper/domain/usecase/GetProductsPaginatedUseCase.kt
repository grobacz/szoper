package com.szopper.domain.usecase

import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsPaginatedUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(limit: Int = 20, offset: Int = 0): Flow<List<Product>> {
        return repository.getProductsPaginated(limit, offset)
    }
}