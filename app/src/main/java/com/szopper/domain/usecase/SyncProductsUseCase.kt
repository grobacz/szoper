package com.szopper.domain.usecase

import com.szopper.data.repository.ProductRepositoryImpl
import com.szopper.data.sync.SyncRepositoryImpl
import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import javax.inject.Inject

class SyncProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val syncRepository: SyncRepositoryImpl
) {
    suspend operator fun invoke(): List<Product>? {
        // Get current local products
        var localProducts: List<Product> = emptyList()
        productRepository.getAllProducts().collect { products ->
            localProducts = products
            return@collect
        }
        
        // Perform sync with remote device
        val syncedProducts = syncRepository.performFullSync(localProducts)
        
        if (syncedProducts != null) {
            // Update local database with synced products
            // This would require additional repository methods to bulk update
            // For now, we return the synced products
            return syncedProducts
        }
        
        return null
    }
}