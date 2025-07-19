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
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.szopper.domain.sync.DeviceInfo
import com.szopper.domain.sync.DeviceType
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
    
    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    
    private val wifiP2pChannel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, Looper.getMainLooper(), null)
    }
    
    private var connectionInfo: WifiP2pInfo? = null
    
    fun discoverPeers(): Flow<List<DeviceInfo>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        if (hasLocationPermission()) {
                            manager?.requestPeers(wifiP2pChannel) { peers ->
                                val deviceList = peers.deviceList.map { device ->
                                    DeviceInfo(
                                        id = device.deviceAddress,
                                        name = device.deviceName ?: "Unknown Device",
                                        type = DeviceType.WIFI_DIRECT,
                                        isAvailable = device.status == WifiP2pDevice.AVAILABLE
                                    )
                                }
                                trySend(deviceList)
                            }
                        }
                    }
                }
            }
        }
        
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        
        context.registerReceiver(receiver, intentFilter)
        
        if (hasLocationPermission()) {
            manager?.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Discovery started successfully
                }
                
                override fun onFailure(reason: Int) {
                    trySend(emptyList())
                }
            })
        }
        
        awaitClose {
            context.unregisterReceiver(receiver)
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
    
    fun getConnectionInfo(): WifiP2pInfo? = connectionInfo
}