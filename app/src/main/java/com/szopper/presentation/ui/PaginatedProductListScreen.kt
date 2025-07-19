package com.szopper.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.szopper.domain.model.Product
import com.szopper.presentation.viewmodel.PaginatedProductListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginatedProductListScreen(
    viewModel: PaginatedProductListViewModel = hiltViewModel(),
    onNavigateToSync: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val hasMoreItems by viewModel.hasMoreItems.collectAsState()
    
    val hapticFeedback = rememberHapticFeedback()
    val listState = rememberLazyListState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }

    // Load more items when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && 
                    lastVisibleItem.index >= products.size - 3 && 
                    hasMoreItems && 
                    !isLoadingMore
                ) {
                    viewModel.loadMoreProducts()
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { 
                Text(
                    "Shopping List (Paginated)",
                    modifier = Modifier.semantics {
                        contentDescription = "Paginated Shopping List application"
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
                        contentDescription = null
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
                        contentDescription = null
                    )
                }
            }
        )

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
            state = listState,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = if (products.isEmpty()) {
                        "Shopping list is empty. Add products using the plus button."
                    } else {
                        "Shopping list with ${products.size} items loaded. ${products.count { it.isBought }} items are marked as bought."
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = products,
                key = { _, product -> product.id.toHexString() }
            ) { index, product ->
                AnimatedProductItem(
                    product = product,
                    index = index,
                    onToggle = { viewModel.toggleProductBought(product.id) }
                )
            }
            
            // Loading more indicator
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Loading more...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // End of list indicator
            if (!hasMoreItems && products.isNotEmpty() && !isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All items loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { 
                hapticFeedback.performHapticFeedback(HapticFeedbackType.CLICK)
                showAddDialog = true 
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
                .semantics {
                    contentDescription = "Add new product to shopping list"
                    role = Role.Button
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )
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
                    singleLine = true,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter product name"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (productName.isNotBlank()) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.SUCCESS)
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
}