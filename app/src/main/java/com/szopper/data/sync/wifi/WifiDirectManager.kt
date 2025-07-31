package com.szopper.data.sync.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
import com.szopper.domain.sync.DiscoveryMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SERVICE_TYPE = "_szopper._tcp"
        private const val SERVICE_NAME = "Szopper"
        private const val SERVICE_PORT = 8888
        private const val TXT_VERSION = "version"
        private const val TXT_APP_VERSION = "1.0"
    }
    
    private val manager: WifiP2pManager? by lazy {
        val mgr = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
        android.util.Log.d("WifiDirectManager", "WiFi P2P Manager initialized: ${mgr != null}")
        mgr
    }
    
    private val wifiP2pChannel: WifiP2pManager.Channel? by lazy {
        val channel = manager?.initialize(context, Looper.getMainLooper(), null)
        android.util.Log.d("WifiDirectManager", "WiFi P2P Channel initialized: ${channel != null}")
        if (manager != null && channel != null) {
            android.util.Log.i("WifiDirectManager", "WiFi Direct is available on this device")
        } else {
            android.util.Log.e("WifiDirectManager", "WiFi Direct initialization failed")
            android.util.Log.e("WifiDirectManager", checkWifiDirectSupport())
        }
        channel
    }
    
    private var connectionInfo: WifiP2pInfo? = null
    private var serviceRegistered = false
    private var isDiscovering = false
    
    fun registerService() {
        android.util.Log.i("WifiDirectManager", "Attempting to register service...")
        android.util.Log.d("WifiDirectManager", "Diagnostics - Location permission: ${hasLocationPermission()}")
        android.util.Log.d("WifiDirectManager", "Diagnostics - Manager available: ${manager != null}")
        android.util.Log.d("WifiDirectManager", "Diagnostics - Channel available: ${wifiP2pChannel != null}")
        android.util.Log.d("WifiDirectManager", "Diagnostics - Service already registered: $serviceRegistered")
        
        if (serviceRegistered) {
            android.util.Log.d("WifiDirectManager", "✓ Service already registered")
            return
        }
        
        if (manager == null) {
            android.util.Log.e("WifiDirectManager", "✗ WiFi P2P Manager not available - WiFi Direct may not be supported on this device")
            return
        }
        
        if (wifiP2pChannel == null) {
            android.util.Log.e("WifiDirectManager", "✗ WiFi P2P Channel not available - Failed to initialize WiFi Direct")
            return
        }
        
        // Don't check permissions for service registration - let it try anyway
        android.util.Log.d("WifiDirectManager", "Proceeding with service registration (ignoring permission check)")
        
        if (!hasLocationPermission()) {
            android.util.Log.w("WifiDirectManager", "⚠ Location permission not granted - service registration may fail")
        }
        
        android.util.Log.i("WifiDirectManager", "Registering Szopper service for discovery...")
        
        try {
            val txtRecord = hashMapOf<String, String>().apply {
                put(TXT_VERSION, TXT_APP_VERSION)
            }
            
            val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_NAME,
                SERVICE_TYPE,
                txtRecord
            )
            
            android.util.Log.d("WifiDirectManager", "Service details: name='$SERVICE_NAME', type='$SERVICE_TYPE', port=$SERVICE_PORT")
            
            manager?.addLocalService(wifiP2pChannel, serviceInfo, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.i("WifiDirectManager", "✓ Service registration successful - device is now discoverable")
                    serviceRegistered = true
                }
                
                override fun onFailure(reason: Int) {
                    val reasonText = when (reason) {
                        0 -> "P2P_UNSUPPORTED - WiFi Direct not supported or disabled"
                        1 -> "ERROR - Generic error" 
                        2 -> "BUSY - WiFi Direct is busy"
                        else -> "UNKNOWN($reason)"
                    }
                    android.util.Log.e("WifiDirectManager", "✗ Service registration failed: $reason ($reasonText)")
                    
                    when (reason) {
                        0 -> {
                            android.util.Log.e("WifiDirectManager", "WiFi Direct is not supported on this device or is disabled.")
                            android.util.Log.e("WifiDirectManager", "Please check: 1) Device supports WiFi Direct, 2) WiFi is enabled, 3) Location services enabled")
                        }
                        1 -> {
                            android.util.Log.e("WifiDirectManager", "Generic WiFi Direct error - may be temporary")
                        }
                        2 -> {
                            android.util.Log.w("WifiDirectManager", "WiFi Direct is busy - will not retry automatically")
                        }
                    }
                    
                    serviceRegistered = false
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("WifiDirectManager", "✗ Exception during service registration: ${e.message}", e)
            serviceRegistered = false
        }
    }
    
    /**
     * Unregister the service
     */
    fun unregisterService() {
        if (!serviceRegistered) return
        
        manager?.clearLocalServices(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                serviceRegistered = false
            }
            
            override fun onFailure(reason: Int) {
                // Service might already be cleared
            }
        })
    }
    
    fun discoverPeers(): Flow<List<DeviceInfo>> = callbackFlow {
        android.util.Log.d("WifiDirectManager", "Starting comprehensive Szopper device discovery")
        val discoveredDevices = mutableMapOf<String, DeviceInfo>()
        
        // Register our service first to make us discoverable
        registerService()
        
        // Service discovery listener for Szopper services ONLY
        val serviceListener = WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, srcDevice ->
            android.util.Log.d("WifiDirectManager", "Service discovered: $instanceName, type: $registrationType from ${srcDevice.deviceName}")
            if (registrationType == SERVICE_TYPE) {
                android.util.Log.i("WifiDirectManager", "✓ Found confirmed Szopper service from ${srcDevice.deviceName ?: instanceName}")
                val deviceInfo = DeviceInfo(
                    id = srcDevice.deviceAddress,
                    name = srcDevice.deviceName ?: instanceName,
                    type = DeviceType.WIFI_DIRECT,
                    isAvailable = srcDevice.status == WifiP2pDevice.AVAILABLE,
                    hasSzopperApp = true,
                    discoveryMethod = DiscoveryMethod.SERVICE
                )
                discoveredDevices[srcDevice.deviceAddress] = deviceInfo
                android.util.Log.d("WifiDirectManager", "Service devices count: ${discoveredDevices.size}")
                trySend(discoveredDevices.values.toList())
            }
        }
        
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomainName, txtRecordMap, srcDevice ->
            android.util.Log.d("WifiDirectManager", "TXT record received from ${srcDevice.deviceName}: $txtRecordMap")
            val version = txtRecordMap[TXT_VERSION]
            if (version != null) {
                val existingDevice = discoveredDevices[srcDevice.deviceAddress]
                if (existingDevice != null) {
                    android.util.Log.d("WifiDirectManager", "Updated device info with version: $version")
                    trySend(discoveredDevices.values.toList())
                }
            }
        }
        
        // Add generic peer discovery as fallback
        val peerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        android.util.Log.d("WifiDirectManager", "Peers changed - requesting peer list")
                        manager?.requestPeers(wifiP2pChannel) { peers ->
                            android.util.Log.d("WifiDirectManager", "Found ${peers.deviceList.size} WiFi Direct peers")
                            peers.deviceList.forEach { device ->
                                if (!discoveredDevices.containsKey(device.deviceAddress)) {
                                    android.util.Log.d("WifiDirectManager", "Adding fallback peer: ${device.deviceName} (${device.deviceAddress})")
                                    val deviceInfo = DeviceInfo(
                                        id = device.deviceAddress,
                                        name = device.deviceName ?: "Unknown Device",
                                        type = DeviceType.WIFI_DIRECT,
                                        isAvailable = device.status == WifiP2pDevice.AVAILABLE,
                                        hasSzopperApp = false, // Unknown - will need to test connection
                                        discoveryMethod = DiscoveryMethod.PEER
                                    )
                                    discoveredDevices[device.deviceAddress] = deviceInfo
                                }
                            }
                            android.util.Log.d("WifiDirectManager", "Total devices after peer discovery: ${discoveredDevices.size}")
                            trySend(discoveredDevices.values.toList())
                        }
                    }
                    WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)
                        val stateText = if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) "STARTED" else "STOPPED"
                        android.util.Log.d("WifiDirectManager", "Discovery state changed: $stateText")
                    }
                }
            }
        }
        
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
        context.registerReceiver(peerReceiver, intentFilter)
        
        if (hasLocationPermission()) {
            android.util.Log.d("WifiDirectManager", "Location permission granted - starting discovery")
            
            // Set up DNS-SD service discovery
            manager?.setDnsSdResponseListeners(wifiP2pChannel, serviceListener, txtListener)
            
            val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
            manager?.addServiceRequest(wifiP2pChannel, serviceRequest, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirectManager", "✓ Service request added successfully")
                }
                
                override fun onFailure(reason: Int) {
                    android.util.Log.e("WifiDirectManager", "✗ Failed to add service request: $reason")
                }
            })
            
            // Start service discovery
            manager?.discoverServices(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirectManager", "✓ Service discovery started successfully")
                }
                
                override fun onFailure(reason: Int) {
                    android.util.Log.e("WifiDirectManager", "✗ Service discovery failed: $reason")
                }
            })
            
            // Also start generic peer discovery as fallback
            manager?.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    android.util.Log.d("WifiDirectManager", "✓ Peer discovery started successfully")
                }
                
                override fun onFailure(reason: Int) {
                    android.util.Log.e("WifiDirectManager", "✗ Peer discovery failed: $reason")
                    // Try to continue with service discovery only
                }
            })
        } else {
            android.util.Log.e("WifiDirectManager", "✗ Location permission not granted - cannot start discovery")
        }
        
        awaitClose {
            android.util.Log.d("WifiDirectManager", "Cleaning up discovery resources")
            isDiscovering = false
            unregisterService()
            manager?.clearServiceRequests(wifiP2pChannel, null)
            manager?.stopPeerDiscovery(wifiP2pChannel, null)
            try {
                context.unregisterReceiver(peerReceiver)
            } catch (e: Exception) {
                android.util.Log.w("WifiDirectManager", "Receiver already unregistered")
            }
        }
    }
    
    suspend fun connectToPeer(deviceInfo: DeviceInfo): Boolean = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val config = WifiP2pConfig().apply {
            deviceAddress = deviceInfo.id
        }
        
        manager?.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                continuation.resume(true)
            }
            
            override fun onFailure(reason: Int) {
                continuation.resume(false)
            }
        })
    }
    
    suspend fun requestConnectionInfo(): WifiP2pInfo? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        manager?.requestConnectionInfo(wifiP2pChannel) { info ->
            connectionInfo = info
            continuation.resume(info)
        }
    }
    
    fun disconnect() {
        manager?.removeGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    
    fun isWifiP2pEnabled(): Boolean {
        return manager != null && wifiP2pChannel != null
    }
    
    fun getDiscoveryInfo(): String {
        return buildString {
            append("WiFi P2P Manager: ${if (manager != null) "Available" else "Not Available"}")
            append(", Channel: ${if (wifiP2pChannel != null) "Initialized" else "Not Initialized"}")
            append(", Location Permission: ${if (hasLocationPermission()) "Granted" else "Denied"}")
            append(", Service Registered: $serviceRegistered")
            append(", Currently Discovering: $isDiscovering")
        }
    }
    
    fun checkWifiDirectSupport(): String {
        return buildString {
            appendLine("=== WiFi Direct Support Check ===")
            appendLine("WiFi P2P Manager: ${if (manager != null) "✓ Available" else "✗ Not Available"}")
            appendLine("WiFi P2P Channel: ${if (wifiP2pChannel != null) "✓ Initialized" else "✗ Failed to initialize"}")
            appendLine("Location Permission: ${if (hasLocationPermission()) "✓ Granted" else "✗ Denied"}")
            appendLine("Service Registration: ${if (serviceRegistered) "✓ Success" else "✗ Failed or not attempted"}")
            
            if (manager == null) {
                appendLine("⚠ WiFi Direct may not be supported on this device")
            }
            if (wifiP2pChannel == null) {
                appendLine("⚠ WiFi Direct initialization failed - check WiFi settings")
            }
            if (!hasLocationPermission()) {
                appendLine("⚠ Location permission required for WiFi Direct discovery")
            }
            
            appendLine("NOTE: Service registration failure does NOT prevent checking existing connections")
            appendLine("=====================================")
        }
    }
    
    fun validateConnection(deviceInfo: DeviceInfo): Boolean {
        android.util.Log.d("WifiDirectManager", "Validating connection capability for ${deviceInfo.name}")
        
        if (!hasLocationPermission()) {
            android.util.Log.e("WifiDirectManager", "✗ Location permission required for connection")
            return false
        }
        
        if (!isWifiP2pEnabled()) {
            android.util.Log.e("WifiDirectManager", "✗ WiFi P2P not available")
            return false
        }
        
        if (!deviceInfo.isAvailable) {
            android.util.Log.w("WifiDirectManager", "⚠ Device marked as unavailable")
            return false
        }
        
        android.util.Log.d("WifiDirectManager", "✓ Connection validation passed for ${deviceInfo.name}")
        return true
    }

    
    fun checkExistingConnections(): Flow<List<DeviceInfo>> = callbackFlow {
        android.util.Log.i("WifiDirectManager", "=== Starting connection check ===")
        android.util.Log.d("WifiDirectManager", "Manager: ${manager != null}, Channel: ${wifiP2pChannel != null}")
        
        val connectedDevices = mutableListOf<DeviceInfo>()
        
        // Always try to get connection info, ignore permission checks
        try {
            if (manager == null) {
                android.util.Log.e("WifiDirectManager", "WiFi P2P Manager is null - cannot check connections")
                trySend(connectedDevices)
                close()
                return@callbackFlow
            }
            
            if (wifiP2pChannel == null) {
                android.util.Log.e("WifiDirectManager", "WiFi P2P Channel is null - cannot check connections")
                trySend(connectedDevices)
                close()
                return@callbackFlow
            }
            
            android.util.Log.d("WifiDirectManager", "Requesting connection info...")
            manager?.requestConnectionInfo(wifiP2pChannel) { connectionInfo ->
                android.util.Log.i("WifiDirectManager", "=== Connection Info Received ===")
                android.util.Log.d("WifiDirectManager", "Group formed: ${connectionInfo?.groupFormed}")
                android.util.Log.d("WifiDirectManager", "Is group owner: ${connectionInfo?.isGroupOwner}")
                android.util.Log.d("WifiDirectManager", "Group owner address: ${connectionInfo?.groupOwnerAddress}")
                
                // If any group is formed, assume there's a connected device
                if (connectionInfo?.groupFormed == true) {
                    android.util.Log.i("WifiDirectManager", "✓ WiFi Direct group is active - SIMPLIFIED: Adding connected device")
                    
                    // SIMPLIFIED: If group exists, add a connected device (assume it's the other device in the group)
                    val connectedDevice = DeviceInfo(
                        id = "wifi_direct_peer",
                        name = "Connected Device",
                        type = DeviceType.WIFI_DIRECT,
                        isAvailable = true,
                        hasSzopperApp = true, // Always assume connected devices have Szopper
                        discoveryMethod = DiscoveryMethod.PEER
                    )
                    connectedDevices.add(connectedDevice)
                    android.util.Log.i("WifiDirectManager", "✓ Added connected WiFi Direct device (simplified detection)")
                    
                    android.util.Log.i("WifiDirectManager", "=== FINAL RESULT: ${connectedDevices.size} connected devices ===")
                    connectedDevices.forEach { device ->
                        android.util.Log.i("WifiDirectManager", "Device: ${device.name} (${device.id}) - Method: ${device.discoveryMethod}")
                    }
                    
                    trySend(connectedDevices)
                    close()
                } else {
                    android.util.Log.d("WifiDirectManager", "No WiFi Direct group is formed")
                    trySend(connectedDevices)
                    close()
                }
            } ?: run {
                android.util.Log.e("WifiDirectManager", "Connection info request returned null immediately")
                trySend(connectedDevices)
                close()
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiDirectManager", "Exception in checkExistingConnections: ${e.message}", e)
            trySend(connectedDevices)
            close()
        }
        
        awaitClose {
            android.util.Log.d("WifiDirectManager", "=== Connection check completed ===")
        }
    }
    
    fun getConnectionInfo(): WifiP2pInfo? = connectionInfo
}