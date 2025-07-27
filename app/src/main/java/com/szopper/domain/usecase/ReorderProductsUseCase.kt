package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import org.mongodb.kbson.ObjectId

class ReorderProductsUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productPositions: List<Pair<ObjectId, Int>>) {
        productRepository.reorderProducts(productPositions)
    }
}
