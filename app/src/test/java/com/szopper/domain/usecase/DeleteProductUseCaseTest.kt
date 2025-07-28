package com.szopper.domain.usecase

import com.szopper.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mongodb.kbson.ObjectId

class DeleteProductUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    @Before
    fun setup() {
        productRepository = mock()
        deleteProductUseCase = DeleteProductUseCase(productRepository)
    }

    @Test
    fun `deleteProduct should call repository deleteProduct`() = runTest {
        // Given
        val productId = ObjectId()

        // When
        deleteProductUseCase(productId)

        // Then
        verify(productRepository).deleteProduct(productId)
    }
}