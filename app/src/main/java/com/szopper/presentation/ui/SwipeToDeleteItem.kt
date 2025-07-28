package com.szopper.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * A composable that wraps content with swipe-to-delete functionality.
 * Swipe left to reveal delete action with Material 3 design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    itemName: String = "Item",
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToStart -> {
                    if (enabled) {
                        onDelete()
                        true
                    } else false
                }
                else -> false
            }
        }
    )
    
    val hapticFeedback = rememberHapticFeedback()
    val coroutineScope = rememberCoroutineScope()
    
    // Track if we've triggered haptic feedback for this swipe
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    
    // Trigger haptic feedback when swipe reaches 30% threshold
    LaunchedEffect(dismissState.progress) {
        val progress = kotlin.math.abs(dismissState.progress)
        if (progress > 0.3f && !hasTriggeredHaptic && enabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LIGHT_IMPACT)
            hasTriggeredHaptic = true
        } else if (progress <= 0.1f) {
            hasTriggeredHaptic = false
        }
    }

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            SwipeDeleteBackground(
                dismissState = dismissState,
                itemName = itemName,
                enabled = enabled
            )
        },
        dismissContent = {
            // Add elevation during swipe
            val elevation by animateDpAsState(
                targetValue = if (kotlin.math.abs(dismissState.progress) > 0.1f) 2.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "swipe_elevation"
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Swipe left to delete $itemName"
                        role = Role.Button
                        onClick(label = "Delete $itemName") {
                            if (enabled) {
                                onDelete()
                                true
                            } else false
                        }
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = elevation)
            ) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(
    dismissState: DismissState,
    itemName: String,
    enabled: Boolean
) {
    val progress = kotlin.math.abs(dismissState.progress)
    val backgroundColor = if (enabled) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    
    // Animate icon appearance
    val iconAlpha by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_alpha"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (progress > 0.3f) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .graphicsLayer {
                    alpha = iconAlpha
                    scaleX = iconScale
                    scaleY = iconScale
                }
        ) {
            if (progress > 0.2f) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor
                )
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete $itemName",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
