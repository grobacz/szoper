package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mongodb.kbson.ObjectId

class ReorderProductsUseCaseTest {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: ReorderProductsUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = ReorderProductsUseCase(repository)
    }

    @Test
    fun `invoke should call repository`() = runTest {
        val productPositions = listOf(ObjectId() to 0, ObjectId() to 1)

        useCase.invoke(productPositions)

        verify(repository).reorderProducts(productPositions)
    }
}
