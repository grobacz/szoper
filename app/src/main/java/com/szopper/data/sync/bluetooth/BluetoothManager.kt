package com.szopper.data.sync.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
class BluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    fun discoverDevices(): Flow<List<DeviceInfo>> = callbackFlow {
        val foundDevices = mutableSetOf<DeviceInfo>()
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (hasBluetoothPermissions()) {
                                val deviceInfo = DeviceInfo(
                                    id = it.address,
                                    name = it.name ?: "Unknown Device",
                                    type = DeviceType.BLUETOOTH,
                                    isAvailable = true
                                )
                                foundDevices.add(deviceInfo)
                                trySend(foundDevices.toList())
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        trySend(foundDevices.toList())
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        
        context.registerReceiver(receiver, filter)
        
        // Add paired devices first
        if (hasBluetoothPermissions()) {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val deviceInfo = DeviceInfo(
                    id = device.address,
                    name = device.name ?: "Unknown Device",
                    type = DeviceType.BLUETOOTH,
                    isAvailable = true
                )
                foundDevices.add(deviceInfo)
            }
            trySend(foundDevices.toList())
            
            // Start discovery
            bluetoothAdapter?.startDiscovery()
        }
        
        awaitClose {
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(receiver)
        }
    }
    
    suspend fun connectToDevice(deviceInfo: DeviceInfo): Boolean = suspendCancellableCoroutine { continuation ->
        if (!hasBluetoothPermissions()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceInfo.id)
            // For now, we'll just check if the device exists
            // Real implementation would establish a socket connection
            continuation.resume(device != null)
        } catch (e: Exception) {
            continuation.resume(false)
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}