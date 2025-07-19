package com.szopper.domain.usecase

import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class GetAllProductsUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var getAllProductsUseCase: GetAllProductsUseCase

    @Before
    fun setup() {
        productRepository = mock()
        getAllProductsUseCase = GetAllProductsUseCase(productRepository)
    }

    @Test
    fun `invoke should return flow of products from repository`() = runTest {
        // Given
        val product1 = Product().apply {
            name = "Product 1"
            isBought = false
        }
        val product2 = Product().apply {
            name = "Product 2"
            isBought = true
        }
        val expectedProducts = listOf(product1, product2)
        whenever(productRepository.getAllProducts()).thenReturn(flowOf(expectedProducts))

        // When
        val result = getAllProductsUseCase().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(expectedProducts, result[0])
    }

    @Test
    fun `invoke should return empty list when no products exist`() = runTest {
        // Given
        whenever(productRepository.getAllProducts()).thenReturn(flowOf(emptyList()))

        // When
        val result = getAllProductsUseCase().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(emptyList(), result[0])
    }
}