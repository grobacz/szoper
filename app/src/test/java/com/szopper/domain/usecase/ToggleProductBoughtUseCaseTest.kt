package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mongodb.kbson.ObjectId

class ToggleProductBoughtUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var toggleProductBoughtUseCase: ToggleProductBoughtUseCase

    @Before
    fun setup() {
        productRepository = mock()
        toggleProductBoughtUseCase = ToggleProductBoughtUseCase(productRepository)
    }

    @Test
    fun `invoke should call repository toggleProductBought with correct id`() = runTest {
        // Given
        val productId = ObjectId()

        // When
        toggleProductBoughtUseCase(productId)

        // Then
        verify(productRepository).toggleProductBought(productId)
    }
}