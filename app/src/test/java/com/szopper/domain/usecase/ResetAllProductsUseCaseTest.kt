package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ResetAllProductsUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var resetAllProductsUseCase: ResetAllProductsUseCase

    @Before
    fun setup() {
        productRepository = mock()
        resetAllProductsUseCase = ResetAllProductsUseCase(productRepository)
    }

    @Test
    fun `invoke should call repository resetAllProducts`() = runTest {
        // When
        resetAllProductsUseCase()

        // Then
        verify(productRepository).resetAllProducts()
    }
}