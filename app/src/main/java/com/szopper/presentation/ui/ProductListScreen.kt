package com.szopper.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.szopper.domain.model.Product
import com.szopper.presentation.viewmodel.ProductListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel = hiltViewModel(),
    onNavigateToSync: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val hapticFeedback = rememberHapticFeedback()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    
    // Track deletions for snackbar
    var lastDeletedProduct by remember { mutableStateOf<Product?>(null) }

    val dragDropState = rememberDragDropState(products) { from, to ->
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LIGHT_IMPACT)
        viewModel.reorderProducts(products.toMutableList().apply {
            add(to, removeAt(from))
        })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Shopping List",
                        modifier = Modifier.semantics {
                            contentDescription = "Shopping List application title"
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.CLICK)
                            onNavigateToSync()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate to device synchronization"
                            role = Role.Button
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null // Already described by button
                        )
                    }
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LONG_PRESS)
                            viewModel.resetAllProducts()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Reset all products to unbought state"
                            role = Role.Button
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null // Already described by button
                        )
                    }
                }
            )
        },
        snackbarHost = {
            UndoSnackbarHost(snackbarHostState = snackbarHostState)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.CLICK)
                    showAddDialog = true
                },
                modifier = Modifier.semantics {
                    contentDescription = "Add new product to shopping list"
                    role = Role.Button
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null // Already described by button
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Loading products from database"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = if (products.isEmpty()) {
                            "Shopping list is empty. Add products using the plus button."
                        } else {
                            "Shopping list with ${products.size} items. ${products.count { it.isBought }} items are marked as bought."
                        }
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = products,
                    key = { _, product -> product.id.toHexString() }
                ) { index, product ->
                    SwipeToDeleteItem(
                        onDelete = {
                            productToDelete = product
                            showDeleteDialog = true
                        },
                        itemName = product.name,
                        enabled = true
                    ) {
                        DraggableItem(
                            state = dragDropState, 
                            index = index,
                            itemHeight = 88f, // Approximate item height including padding
                            onDragStart = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LONG_PRESS)
                            },
                            onDragEnd = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LIGHT_IMPACT)
                            }
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 8.dp else 0.dp,
                                label = "drag_elevation"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.02f else 1f,
                                label = "drag_scale"
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = if (index == dragDropState.draggingItemIndex.value) {
                                            dragDropState.draggingItemOffset.value
                                        } else 0f
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                            ) {
                                AnimatedProductItem(
                                    product = product,
                                    index = index,
                                    onToggle = { viewModel.toggleProductBought(product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                productName = ""
            },
            title = { Text("Add Product") },
            text = {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (productName.isNotBlank()) {
                            viewModel.addProduct(productName)
                            showAddDialog = false
                            productName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        productName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    DeleteConfirmationDialog(
        isVisible = showDeleteDialog,
        itemName = productToDelete?.name ?: "",
        onConfirmDelete = {
            productToDelete?.let { product ->
                viewModel.deleteProduct(product)
                lastDeletedProduct = product
            }
        },
        onDismiss = {
            showDeleteDialog = false
            productToDelete = null
        }
    )
    
    // Show undo snackbar when a product is deleted
    LaunchedEffect(lastDeletedProduct) {
        lastDeletedProduct?.let { product ->
            val result = snackbarHostState.showUndoSnackbar(
                itemName = product.name,
                onUndo = { viewModel.undoDelete(product.id) }
            )
            lastDeletedProduct = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedProductItem(
    product: Product,
    index: Int,
    onToggle: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val hapticFeedback = rememberHapticFeedback()

    // Staggered animation for list items could be implemented here if needed
    // val animationDelay = index * 50

    // Scale animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "item_scale"
    )

    // Fade animation for bought state
    val alpha by animateFloatAsState(
        targetValue = if (product.isBought) 0.7f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "item_alpha"
    )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .semantics {
                contentDescription = "Item ${index + 1}: ${product.name}, ${if (product.isBought) "bought" else "not bought"}"
                stateDescription = if (product.isBought) "Completed" else "Not completed"
                role = Role.Checkbox
            },
        onClick = {
            isPressed = true
            hapticFeedback.performHapticFeedback(
                if (product.isBought) HapticFeedbackType.LIGHT_IMPACT
                else HapticFeedbackType.SUCCESS
            )
            onToggle()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedCheckbox(
                checked = product.isBought,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            AnimatedText(
                text = product.name,
                isBought = product.isBought,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Checkbox with subtle scale animation
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "checkbox_scale"
    )

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier
            .scale(scale)
            .semantics {
                contentDescription = if (checked) "Mark as not bought" else "Mark as bought"
                role = Role.Checkbox
            }
    )
}

@Composable
fun AnimatedText(
    text: String,
    isBought: Boolean,
    modifier: Modifier = Modifier
) {
    // Crossfade animation for text decoration changes
    Crossfade(
        targetState = isBought,
        animationSpec = tween(durationMillis = 200),
        label = "text_decoration"
    ) { bought ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (bought) TextDecoration.LineThrough else null,
            color = if (bought) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = modifier
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onToggle: () -> Unit
) {
    AnimatedProductItem(
        product = product,
        index = 0,
        onToggle = onToggle
    )
}
