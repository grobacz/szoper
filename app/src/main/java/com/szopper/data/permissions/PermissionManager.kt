package com.szopper.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Required permissions for WiFi Direct functionality
     */
    val wifiDirectPermissions: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )
    
    
    /**
     * All sync-related permissions (WiFi Direct only)
     */
    val allSyncPermissions: List<String>
        get() = wifiDirectPermissions
    
    /**
     * Check if all WiFi Direct permissions are granted
     */
    fun hasWifiDirectPermissions(): Boolean {
        return wifiDirectPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    
    /**
     * Check if all sync permissions are granted
     */
    fun hasAllSyncPermissions(): Boolean {
        return hasWifiDirectPermissions()
    }
    
    /**
     * Get list of missing WiFi Direct permissions
     */
    fun getMissingWifiDirectPermissions(): List<String> {
        return wifiDirectPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    
    /**
     * Get list of all missing sync permissions
     */
    fun getMissingSyncPermissions(): List<String> {
        return allSyncPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if location services are enabled (required for WiFi Direct)
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    
    /**
     * Get user-friendly explanation for a permission
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "Location access is required to discover nearby devices using WiFi Direct. This helps you find other Szopper users to sync with."
            
            
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE -> 
                "WiFi access is required to use WiFi Direct for fast, direct device-to-device communication."
            
            else -> "This permission is required for device synchronization functionality."
        }
    }
    
    /**
     * Data class representing the current permission state
     */
    data class PermissionState(
        val hasWifiDirectPermissions: Boolean,
        val isLocationEnabled: Boolean,
        val missingPermissions: List<String>,
        val canDiscoverDevices: Boolean
    ) {
        val hasAllRequiredPermissions: Boolean
            get() = hasWifiDirectPermissions
        
        val isFullyReady: Boolean
            get() = hasAllRequiredPermissions && isLocationEnabled
    }
    
    /**
     * Get current permission state for sync functionality
     */
    fun getCurrentPermissionState(): PermissionState {
        val hasWifiDirect = hasWifiDirectPermissions()
        val locationEnabled = isLocationEnabled()
        val missing = getMissingSyncPermissions()
        
        return PermissionState(
            hasWifiDirectPermissions = hasWifiDirect,
            isLocationEnabled = locationEnabled,
            missingPermissions = missing,
            canDiscoverDevices = hasWifiDirect && locationEnabled
        )
    }
}