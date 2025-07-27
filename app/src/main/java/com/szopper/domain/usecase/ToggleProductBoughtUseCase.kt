package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

class ToggleProductBoughtUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(id: ObjectId) {
        repository.toggleProductBought(id)
    }
}
