package com.szopper.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.szopper.domain.sync.ConnectionStatus
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.SyncRepository
import com.szopper.domain.usecase.GetHapticFeedbackSettingUseCase
import com.szopper.domain.usecase.SetHapticFeedbackSettingUseCase
import com.szopper.domain.usecase.SyncProductsUseCase
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
    private val setHapticFeedbackSettingUseCase: SetHapticFeedbackSettingUseCase
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
    
    init {
        observeConnectionStatus()
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
        viewModelScope.launch {
            _isDiscovering.value = true
            _error.value = null
            
            syncRepository.discoverDevices()
                .catch { throwable ->
                    _error.value = throwable.message
                    _isDiscovering.value = false
                }
                .collect { devices ->
                    _discoveredDevices.value = devices
                    if (_connectionStatus.value != ConnectionStatus.DISCOVERING) {
                        _isDiscovering.value = false
                    }
                }
        }
    }
    
    fun connectToDevice(deviceInfo: DeviceInfo) {
        viewModelScope.launch {
            try {
                val connected = syncRepository.connectToDevice(deviceInfo)
                if (!connected) {
                    _error.value = "Failed to connect to ${deviceInfo.name}"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun syncProducts() {
        viewModelScope.launch {
            try {
                val syncedProducts = syncProductsUseCase()
                if (syncedProducts == null) {
                    _error.value = "Failed to sync products"
                }
                // Note: In a real implementation, we would update the local database
                // with the synced products and notify the UI
            } catch (e: Exception) {
                _error.value = e.message
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