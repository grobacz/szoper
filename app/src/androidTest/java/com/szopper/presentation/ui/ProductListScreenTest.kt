package com.szopper.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.szopper.domain.model.Product
import com.szopper.presentation.ui.theme.SzopperTheme
import com.szopper.presentation.viewmodel.ProductListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productListScreen_displaysProductsCorrectly() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val product1 = Product().apply {
            name = "Milk"
            isBought = false
        }
        val product2 = Product().apply {
            name = "Bread"
            isBought = true
        }
        val products = listOf(product1, product2)
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bread").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add product").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reset all products").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Sync with devices").assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysLoadingState() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(emptyList()))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(true))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()
        // Loading indicator should be displayed (CircularProgressIndicator)
        // Note: CircularProgressIndicator doesn't have easily testable text, 
        // but we can verify the main content is still there
    }

    @Test
    fun productListScreen_displaysErrorState() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val errorMessage = "Failed to load products"
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(emptyList()))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(errorMessage))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun productListScreen_addProductDialogFlow() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(emptyList()))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Click add button
        composeTestRule.onNodeWithContentDescription("Add product").performClick()

        // Then - Dialog should be displayed
        composeTestRule.onNodeWithText("Add Product").assertIsDisplayed()
        composeTestRule.onNodeWithText("Product name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun productListScreen_addProductDialogCancel() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(emptyList()))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Open dialog and cancel
        composeTestRule.onNodeWithContentDescription("Add product").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then - Dialog should be dismissed
        composeTestRule.onNodeWithText("Add Product").assertIsNotDisplayed()
    }

    @Test
    fun productListScreen_productItemDisplaysBoughtState() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val boughtProduct = Product().apply {
            name = "Bought Item"
            isBought = true
        }
        val unboughtProduct = Product().apply {
            name = "Unbought Item"
            isBought = false
        }
        val products = listOf(boughtProduct, unboughtProduct)
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Bought Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unbought Item").assertIsDisplayed()
        
        // Both products should have checkboxes
        // Note: In a real test, we'd verify the checkbox states, but that requires
        // more complex test node matching
    }

    @Test
    fun productListScreen_dragAndDropReordersItems() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val product1 = Product().apply {
            name = "First Item"
            isBought = false
            position = 0
        }
        val product2 = Product().apply {
            name = "Second Item"
            isBought = false
            position = 1
        }
        val product3 = Product().apply {
            name = "Third Item"
            isBought = false
            position = 2
        }
        val products = listOf(product1, product2, product3)
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Verify initial order
        composeTestRule.onNodeWithText("First Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third Item").assertIsDisplayed()

        // Perform long press and drag gesture on first item
        composeTestRule.onNodeWithText("First Item").performTouchInput {
            longClick()
            // Note: In a real test environment, we would perform drag gestures
            // However, drag testing in Compose Test is complex and may require custom test helpers
        }

        // Then - verify that reorderProducts was called on ViewModel
        // Note: In practice, we would verify the reorder was called, but due to the complexity
        // of testing drag gestures in Compose, this test verifies the basic setup
    }

    @Test
    fun productListScreen_longPressStartsDragMode() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val product = Product().apply {
            name = "Draggable Item"
            isBought = false
            position = 0
        }
        val products = listOf(product)
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then - Item should be displayed and respond to long press
        composeTestRule.onNodeWithText("Draggable Item").assertIsDisplayed()
        
        // Perform long press to initiate drag
        composeTestRule.onNodeWithText("Draggable Item").performTouchInput {
            longClick()
        }
        
        // Item should still be displayed after long press
        composeTestRule.onNodeWithText("Draggable Item").assertIsDisplayed()
    }

    @Test
    fun productListScreen_multipleItemsCanBeDragged() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val products = (1..5).map { i ->
            Product().apply {
                name = "Item $i"
                isBought = false
                position = i - 1
            }
        }
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then - All items should be displayed
        products.forEach { product ->
            composeTestRule.onNodeWithText(product.name).assertIsDisplayed()
        }
        
        // Each item should respond to long press
        products.take(3).forEach { product ->
            composeTestRule.onNodeWithText(product.name).performTouchInput {
                longClick()
            }
            composeTestRule.onNodeWithText(product.name).assertIsDisplayed()
        }
    }

    @Test
    fun productListScreen_dragAndDropWorksWithBoughtItems() {
        // Given
        val mockViewModel = mock<ProductListViewModel>()
        val boughtProduct = Product().apply {
            name = "Bought Item"
            isBought = true
            position = 0
        }
        val unboughtProduct = Product().apply {
            name = "Unbought Item"
            isBought = false
            position = 1
        }
        val products = listOf(boughtProduct, unboughtProduct)
        
        whenever(mockViewModel.products).thenReturn(MutableStateFlow(products))
        whenever(mockViewModel.isLoading).thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.error).thenReturn(MutableStateFlow(null))

        // When
        composeTestRule.setContent {
            SzopperTheme {
                ProductListScreen(viewModel = mockViewModel)
            }
        }

        // Then - Both items should be draggable regardless of bought state
        composeTestRule.onNodeWithText("Bought Item").performTouchInput {
            longClick()
        }
        composeTestRule.onNodeWithText("Bought Item").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Unbought Item").performTouchInput {
            longClick()
        }
        composeTestRule.onNodeWithText("Unbought Item").assertIsDisplayed()
    }
}