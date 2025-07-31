package com.szopper.presentation.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog for requesting sync-related permissions with clear explanations
 */
@Composable
fun PermissionRequestDialog(
    isVisible: Boolean,
    permissionType: PermissionType,
    permissionRationale: String,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = permissionType.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = permissionType.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = permissionRationale,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    if (permissionType == PermissionType.LOCATION_DISABLED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please enable location services in your device settings to continue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        if (permissionType == PermissionType.LOCATION_DISABLED) {
                            onOpenSettings()
                        } else {
                            onRequestPermission()
                        }
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (permissionType == PermissionType.LOCATION_DISABLED) {
                            "Open Settings"
                        } else {
                            "Grant Permission"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Not Now")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

/**
 * Different types of permission requests
 */
enum class PermissionType(
    val title: String,
    val icon: ImageVector
) {
    WIFI_DIRECT(
        title = "WiFi Direct Access",
        icon = Icons.Default.Settings
    ),
    LOCATION(
        title = "Location Access",
        icon = Icons.Default.LocationOn
    ),
    LOCATION_DISABLED(
        title = "Location Services Required",
        icon = Icons.Default.LocationOn
    ),
    ALL_SYNC_PERMISSIONS(
        title = "Sync Permissions Required",
        icon = Icons.Default.Settings
    )
}

/**
 * Permission status information card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    actionText: String = if (isGranted) "Granted" else "Grant"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    }
                )
            }
            
            if (!isGranted) {
                FilledTonalButton(
                    onClick = onActionClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = actionText)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Permission granted",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}