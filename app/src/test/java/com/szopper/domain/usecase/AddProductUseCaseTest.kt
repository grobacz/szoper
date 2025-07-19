package com.szopper.domain.usecase

import com.szopper.domain.model.Product
import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AddProductUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var addProductUseCase: AddProductUseCase

    @Before
    fun setup() {
        productRepository = mock()
        addProductUseCase = AddProductUseCase(productRepository)
    }

    @Test
    fun `invoke with valid product name should add product`() = runTest {
        // Given
        val productName = "Test Product"
        val expectedProduct = Product().apply {
            name = productName
            isBought = false
        }
        whenever(productRepository.addProduct(productName)).thenReturn(expectedProduct)

        // When
        val result = addProductUseCase(productName)

        // Then
        verify(productRepository).addProduct(productName)
        assertEquals(expectedProduct.name, result.name)
        assertEquals(false, result.isBought)
    }

    @Test
    fun `invoke with blank product name should throw exception`() = runTest {
        // Given
        val blankName = "   "

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            addProductUseCase(blankName)
        }
    }

    @Test
    fun `invoke with empty product name should throw exception`() = runTest {
        // Given
        val emptyName = ""

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            addProductUseCase(emptyName)
        }
    }

    @Test
    fun `invoke should trim whitespace from product name`() = runTest {
        // Given
        val productNameWithSpaces = "  Test Product  "
        val trimmedName = "Test Product"
        val expectedProduct = Product().apply {
            name = trimmedName
            isBought = false
        }
        whenever(productRepository.addProduct(trimmedName)).thenReturn(expectedProduct)

        // When
        addProductUseCase(productNameWithSpaces)

        // Then
        verify(productRepository).addProduct(trimmedName)
    }
}