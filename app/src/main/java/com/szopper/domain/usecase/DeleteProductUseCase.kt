package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

/**
 * Use case for deleting a product from the shopping list.
 * Also handles position reindexing for remaining items.
 */
class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    /**
     * Deletes a product by its ID and reindexes positions of remaining items.
     * 
     * @param productId The ObjectId of the product to delete
     * @throws Exception if the product doesn't exist or deletion fails
     */
    suspend operator fun invoke(productId: ObjectId) {
        productRepository.deleteProduct(productId)
    }
}