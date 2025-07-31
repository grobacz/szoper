package com.szopper.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szopper.data.permissions.PermissionManager
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.SyncRepository
import com.szopper.domain.usecase.GetHapticFeedbackSettingUseCase
import com.szopper.domain.usecase.SetHapticFeedbackSettingUseCase
import com.szopper.domain.usecase.SyncProductsUseCase
import com.szopper.presentation.permissions.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncProductsUseCase: SyncProductsUseCase,
    private val getHapticFeedbackSettingUseCase: GetHapticFeedbackSettingUseCase,
    private val setHapticFeedbackSettingUseCase: SetHapticFeedbackSettingUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _hapticFeedbackEnabled = MutableStateFlow(getHapticFeedbackSettingUseCase())
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()
    
    // Permission-related state
    private val _permissionState = MutableStateFlow(permissionManager.getCurrentPermissionState())
    val permissionState: StateFlow<PermissionManager.PermissionState> = _permissionState.asStateFlow()
    
    private val _showPermissionDialog = MutableStateFlow<PermissionType?>(null)
    val showPermissionDialog: StateFlow<PermissionType?> = _showPermissionDialog.asStateFlow()
    
    private val _pendingPermissions = MutableStateFlow<List<String>>(emptyList())
    val pendingPermissions: StateFlow<List<String>> = _pendingPermissions.asStateFlow()
    
    init {
        observeConnectionStatus()
        checkPermissions()
    }
    
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            syncRepository.getConnectionStatus()
                .catch { throwable ->
                    _error.value = throwable.message
                }
                .collect { status ->
                    _connectionStatus.value = status
                }
        }
    }
    
    fun startDiscovery() {
        // Check permissions before starting discovery
        if (!checkAndRequestPermissions()) {
            return
        }
        
        val currentState = permissionManager.getCurrentPermissionState()
        android.util.Log.d("SyncViewModel", "Starting discovery with permissions: " +
            "WiFi=${currentState.hasWifiDirectPermissions}, " +
            "Location=${currentState.isLocationEnabled}, " +
            "Ready=${currentState.isFullyReady}")
        
        viewModelScope.launch {
            _isDiscovering.value = true
            _error.value = null
            
            // Add a timeout for discovery
            val discoveryTimeoutJob = launch {
                kotlinx.coroutines.delay(15000) // 15 seconds timeout
                if (_isDiscovering.value) {
                    android.util.Log.d("SyncViewModel", "Discovery timeout reached, stopping discovery")
                    _isDiscovering.value = false
                    if (_connectionStatus.value == ConnectionStatus.DISCOVERING) {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    }
                }
            }
            
            syncRepository.discoverDevices()
                .catch { throwable ->
                    android.util.Log.e("SyncViewModel", "Discovery error: ${throwable.message}", throwable)
                    _error.value = throwable.message
                    _isDiscovering.value = false
                    discoveryTimeoutJob.cancel()
                }
                .collect { devices ->
                    android.util.Log.d("SyncViewModel", "Discovered ${devices.size} devices:")
                    devices.forEach { device ->
                        android.util.Log.d("SyncViewModel", "  - ${device.name} (${device.type}) - Available: ${device.isAvailable}")
                    }
                    _discoveredDevices.value = devices
                    
                    // Stop the discovering spinner after we have some results or after initial discovery
                    if (devices.isNotEmpty() || _connectionStatus.value != ConnectionStatus.DISCOVERING) {
                        _isDiscovering.value = false
                        if (_connectionStatus.value == ConnectionStatus.DISCOVERING) {
                            _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        }
                        discoveryTimeoutJob.cancel()
                    }
                }
        }
    }
    
    /**
     * Check current permission state and update UI accordingly
     */
    private fun checkPermissions() {
        _permissionState.value = permissionManager.getCurrentPermissionState()
    }
    
    /**
     * Check if all required permissions are granted, request if not
     * @return true if all permissions are available, false if permission request is needed
     */
    private fun checkAndRequestPermissions(): Boolean {
        val currentState = permissionManager.getCurrentPermissionState()
        _permissionState.value = currentState
        
        when {
            !currentState.isLocationEnabled -> {
                _showPermissionDialog.value = PermissionType.LOCATION_DISABLED
                return false
            }
            !currentState.hasWifiDirectPermissions -> {
                _pendingPermissions.value = permissionManager.getMissingWifiDirectPermissions()
                _showPermissionDialog.value = PermissionType.WIFI_DIRECT
                return false
            }
            else -> return true
        }
    }
    
    /**
     * Handle permission request result
     */
    fun onPermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        
        if (allGranted) {
            checkPermissions()
            // If all permissions granted and services are enabled, start discovery
            if (_permissionState.value.isFullyReady) {
                startDiscovery()
            }
        } else {
            _error.value = "Permissions are required for device discovery. Please grant the requested permissions."
        }
        
        dismissPermissionDialog()
    }
    
    /**
     * Get permission rationale text for current pending permissions
     */
    fun getPermissionRationale(): String {
        val missingPermissions = _pendingPermissions.value
        if (missingPermissions.isEmpty()) return ""
        
        return when {
            missingPermissions.any { it.contains("location", true) } ->
                permissionManager.getPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
            missingPermissions.any { it.contains("bluetooth", true) } ->
                permissionManager.getPermissionRationale(android.Manifest.permission.BLUETOOTH_SCAN)
            else -> "Permissions are required for device synchronization."
        }
    }
    
    /**
     * Dismiss permission dialog
     */
    fun dismissPermissionDialog() {
        _showPermissionDialog.value = null
        _pendingPermissions.value = emptyList()
    }
    
    /**
     * Refresh permission state (call when returning from settings)
     */
    fun refreshPermissionState() {
        checkPermissions()
    }
    
    fun connectToDevice(deviceInfo: DeviceInfo) {
        android.util.Log.d("SyncViewModel", "Attempting to connect to device: ${deviceInfo.name} (${deviceInfo.type})")
        viewModelScope.launch {
            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                val connected = syncRepository.connectToDevice(deviceInfo)
                if (!connected) {
                    android.util.Log.e("SyncViewModel", "Failed to connect to ${deviceInfo.name}")
                    _error.value = "Failed to connect to ${deviceInfo.name}"
                } else {
                    android.util.Log.d("SyncViewModel", "Successfully connected to ${deviceInfo.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncViewModel", "Exception connecting to ${deviceInfo.name}: ${e.message}", e)
                _error.value = e.message
            }
        }
    }
    
    fun syncProducts() {
        android.util.Log.d("SyncViewModel", "=== Starting product sync ===")
        viewModelScope.launch {
            try {
                _connectionStatus.value = ConnectionStatus.SYNCING
                
                // Find the connected device from our discovered devices list
                val connectedDevice = _discoveredDevices.value.find { 
                    it.discoveryMethod == com.szopper.domain.sync.DiscoveryMethod.PEER 
                }
                
                if (connectedDevice == null) {
                    android.util.Log.e("SyncViewModel", "No connected device found for sync")
                    _error.value = "No connected device found"
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    return@launch
                }
                
                android.util.Log.d("SyncViewModel", "Found connected device: ${connectedDevice.name}")
                
                // Check if we already have a data connection established
                val alreadyConnected = _connectionStatus.value == ConnectionStatus.CONNECTED
                
                if (!alreadyConnected) {
                    android.util.Log.d("SyncViewModel", "Establishing data connection first...")
                    val connected = syncRepository.connectToDevice(connectedDevice)
                    if (!connected) {
                        android.util.Log.e("SyncViewModel", "Failed to establish data connection for sync")
                        _error.value = "Failed to establish connection for sync"
                        _connectionStatus.value = ConnectionStatus.ERROR
                        return@launch
                    }
                    android.util.Log.d("SyncViewModel", "Data connection established successfully")
                }
                
                // Now perform the actual sync
                android.util.Log.d("SyncViewModel", "Performing product synchronization...")
                val syncedProducts = syncProductsUseCase()
                if (syncedProducts == null) {
                    android.util.Log.e("SyncViewModel", "Product sync failed")
                    _error.value = "Failed to sync products - no data received"
                    _connectionStatus.value = ConnectionStatus.ERROR
                } else {
                    android.util.Log.i("SyncViewModel", "✓ Product sync completed successfully - ${syncedProducts.size} products")
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    // Note: In a real implementation, we would update the local database
                    // with the synced products and notify the UI
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncViewModel", "Exception during product sync: ${e.message}", e)
                _error.value = "Sync failed: ${e.message}"
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            try {
                syncRepository.disconnect()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun setHapticFeedbackEnabled(enabled: Boolean) {
        setHapticFeedbackSettingUseCase(enabled)
        _hapticFeedbackEnabled.value = enabled
    }
}