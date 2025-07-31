package com.szopper.presentation.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.DiscoveryMethod
import com.szopper.presentation.permissions.PermissionRequestDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val error by viewModel.error.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    
    // Permission-related state
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val pendingPermissions by viewModel.pendingPermissions.collectAsState()
    
    // Permission launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onPermissionResult(permissions)
    }
    
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { 
        // When returning from settings, refresh permission state
        viewModel.refreshPermissionState()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Device Sync") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> {
                        IconButton(onClick = { viewModel.syncProducts() }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Sync products"
                            )
                        }
                    }
                    ConnectionStatus.DISCONNECTED -> {
                        IconButton(onClick = { viewModel.startDiscovery() }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Discover devices"
                            )
                        }
                    }
                    else -> {}
                }
            }
        )
        
        // Connection Status
        ConnectionStatusCard(
            status = connectionStatus,
            onDisconnect = { viewModel.disconnect() }
        )
        
        // Error Display
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
        
        // Discovery Loading
        if (isDiscovering) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Discovering devices...")
                }
            }
        }
        
        // Device List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            items(discoveredDevices) { device ->
                DeviceItem(
                    device = device,
                    onConnect = { viewModel.connectToDevice(device) },
                    onSync = { viewModel.syncProducts() },
                    isConnectable = connectionStatus == ConnectionStatus.DISCONNECTED
                )
            }
            
            if (discoveredDevices.isEmpty() && !isDiscovering) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No devices found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.startDiscovery() }
                            ) {
                                Text("Start Discovery")
                            }
                        }
                    }
                }
            }
        }
        
        // Settings Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
                    )
                }
            }
        }
    }
    
    // Permission Dialog
    showPermissionDialog?.let { permissionType ->
        PermissionRequestDialog(
            isVisible = true,
            permissionType = permissionType,
            permissionRationale = viewModel.getPermissionRationale(),
            onRequestPermission = {
                // Request the actual permissions through the Android system
                permissionLauncher.launch(pendingPermissions.toTypedArray())
            },
            onDismiss = {
                viewModel.dismissPermissionDialog()
            },
            onOpenSettings = {
                // Open Android settings for location services or app permissions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                settingsLauncher.launch(intent)
                viewModel.dismissPermissionDialog()
            }
        )
    }
}

@Composable
fun ConnectionStatusCard(
    status: ConnectionStatus,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                ConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = status.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (status == ConnectionStatus.CONNECTED) {
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: DeviceInfo,
    onConnect: () -> Unit,
    onSync: (() -> Unit)? = null, // Add sync callback
    isConnectable: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${device.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Device info row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Szopper app indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (device.hasSzopperApp) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (device.hasSzopperApp) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (device.hasSzopperApp) "Szopper" else "Unknown App",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (device.hasSzopperApp) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        // Discovery method indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (device.discoveryMethod) {
                                    DiscoveryMethod.SERVICE -> Icons.Filled.Settings
                                    DiscoveryMethod.PEER -> Icons.Filled.CheckCircle // Connected peer
                                    DiscoveryMethod.MANUAL -> Icons.Filled.Edit
                                },
                                contentDescription = null,
                                tint = if (device.discoveryMethod == DiscoveryMethod.PEER) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when (device.discoveryMethod) {
                                    DiscoveryMethod.SERVICE -> "Service"
                                    DiscoveryMethod.PEER -> "Connected" // Connected peer
                                    DiscoveryMethod.MANUAL -> "Manual"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (device.discoveryMethod == DiscoveryMethod.PEER) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Availability indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (device.isAvailable) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (device.isAvailable) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (device.isAvailable) "Available" else "Busy",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (device.isAvailable) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Show sync button for connected devices, connect button for others
                if (device.discoveryMethod == DiscoveryMethod.PEER && onSync != null) {
                    Button(
                        onClick = onSync,
                        enabled = device.isAvailable && device.hasSzopperApp,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Sync Products")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = isConnectable && device.isAvailable,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (device.hasSzopperApp) "Connect" else "Test Connect"
                        )
                    }
                }
            }
        }
    }
}
