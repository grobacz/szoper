package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mongodb.kbson.ObjectId

class ReorderProductsUseCaseTest {

    @Test
    fun `invoke should call repository`() = runBlocking {
        val repository = mockk<ProductRepository>(relaxed = true)
        val useCase = ReorderProductsUseCase(repository)
        val productPositions = listOf(ObjectId() to 0, ObjectId() to 1)

        useCase.invoke(productPositions)

        coVerify { repository.reorderProducts(productPositions) }
    }
}
