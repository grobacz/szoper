package com.szopper.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

/**
 * Data class representing an undo action for deleted items
 */
data class UndoAction(
    val itemName: String,
    val onUndo: () -> Unit,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Composable that manages undo snackbar state and display
 */
@Composable
fun UndoSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        UndoSnackbar(
            snackbarData = snackbarData
        )
    }
}

/**
 * Custom snackbar with Material 3 design for undo actions
 */
@Composable
private fun UndoSnackbar(
    snackbarData: SnackbarData
) {
    Snackbar(
        modifier = androidx.compose.ui.Modifier.semantics {
            contentDescription = "Item deleted. Swipe to dismiss or tap undo to restore."
        },
        action = {
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    modifier = androidx.compose.ui.Modifier.semantics {
                        contentDescription = "Undo delete action"
                        role = Role.Button
                    }
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissAction = {
            IconButton(
                onClick = { snackbarData.dismiss() },
                modifier = androidx.compose.ui.Modifier.semantics {
                    contentDescription = "Dismiss undo notification"
                    role = Role.Button
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionContentColor = MaterialTheme.colorScheme.inversePrimary,
        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Text(
            text = snackbarData.visuals.message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Extension function to show undo snackbar with proper Material 3 styling
 */
suspend fun SnackbarHostState.showUndoSnackbar(
    itemName: String,
    onUndo: () -> Unit
): SnackbarResult {
    return showSnackbar(
        message = "$itemName deleted",
        actionLabel = "Undo",
        withDismissAction = true,
        duration = SnackbarDuration.Long
    ).also { result ->
        if (result == SnackbarResult.ActionPerformed) {
            onUndo()
        }
    }
}