package com.szopper.presentation.viewmodel

import com.szopper.domain.model.Product
import com.szopper.domain.usecase.AddProductUseCase
import com.szopper.domain.usecase.GetAllProductsUseCase
import com.szopper.domain.usecase.ReorderProductsUseCase
import com.szopper.domain.usecase.ResetAllProductsUseCase
import com.szopper.domain.usecase.ToggleProductBoughtUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mongodb.kbson.ObjectId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private lateinit var getAllProductsUseCase: GetAllProductsUseCase
    private lateinit var addProductUseCase: AddProductUseCase
    private lateinit var toggleProductBoughtUseCase: ToggleProductBoughtUseCase
    private lateinit var resetAllProductsUseCase: ResetAllProductsUseCase
    private lateinit var reorderProductsUseCase: ReorderProductsUseCase
    private lateinit var viewModel: ProductListViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getAllProductsUseCase = mock()
        addProductUseCase = mock()
        toggleProductBoughtUseCase = mock()
        resetAllProductsUseCase = mock()
        reorderProductsUseCase = mock()
        
        // Setup default empty flow
        whenever(getAllProductsUseCase()).thenReturn(flowOf(emptyList()))
        
        viewModel = ProductListViewModel(
            getAllProductsUseCase,
            addProductUseCase,
            toggleProductBoughtUseCase,
            resetAllProductsUseCase,
            reorderProductsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty products list and no loading`() = runTest {
        // Given - ViewModel is initialized with empty flow

        // Then
        assertEquals(emptyList(), viewModel.products.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `should load products from use case on initialization`() = runTest {
        // Given
        val product1 = Product().apply { name = "Product 1" }
        val product2 = Product().apply { name = "Product 2" }
        val products = listOf(product1, product2)
        whenever(getAllProductsUseCase()).thenReturn(flowOf(products))

        // When
        val newViewModel = ProductListViewModel(
            getAllProductsUseCase,
            addProductUseCase,
            toggleProductBoughtUseCase,
            resetAllProductsUseCase,
            reorderProductsUseCase
        )

        // Then
        assertEquals(products, newViewModel.products.value)
        assertFalse(newViewModel.isLoading.value)
        assertNull(newViewModel.error.value)
    }

    @Test
    fun `addProduct should call use case with product name`() = runTest {
        // Given
        val productName = "New Product"
        val newProduct = Product().apply { name = productName }
        whenever(addProductUseCase(productName)).thenReturn(newProduct)

        // When
        viewModel.addProduct(productName)

        // Then
        verify(addProductUseCase).invoke(productName)
    }

    @Test
    fun `addProduct should set error when use case throws exception`() = runTest {
        // Given
        val productName = "New Product"
        val errorMessage = "Failed to add product"
        whenever(addProductUseCase(productName)).thenThrow(RuntimeException(errorMessage))

        // When
        viewModel.addProduct(productName)

        // Then
        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `toggleProductBought should call use case with product id`() = runTest {
        // Given
        val productId = ObjectId()

        // When
        viewModel.toggleProductBought(productId)

        // Then
        verify(toggleProductBoughtUseCase).invoke(productId)
    }

    @Test
    fun `resetAllProducts should call use case`() = runTest {
        // When
        viewModel.resetAllProducts()

        // Then
        verify(resetAllProductsUseCase).invoke()
    }

    @Test
    fun `reorderProducts should call use case with product positions`() = runTest {
        // Given
        val product1 = Product().apply { id = ObjectId() }
        val product2 = Product().apply { id = ObjectId() }
        val products = listOf(product1, product2)
        val expectedPositions = listOf(product1.id to 0, product2.id to 1)

        // When
        viewModel.reorderProducts(products)

        // Then
        verify(reorderProductsUseCase).invoke(expectedPositions)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given - Set an error first
        viewModel.addProduct("") // This should cause an error if validation fails

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.error.value)
    }
}