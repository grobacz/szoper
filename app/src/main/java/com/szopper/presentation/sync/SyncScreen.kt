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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val error by viewModel.error.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    
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
    isConnectable: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isConnectable) { onConnect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (device.type) {
                    DeviceType.WIFI_DIRECT -> Icons.Filled.Star
                    DeviceType.BLUETOOTH -> Icons.Filled.Build
                },
                contentDescription = device.type.name,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${device.type.name.replace("_", " ")} • ${device.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (device.isAvailable && isConnectable) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Connect")
                }
            }
        }
    }
}
