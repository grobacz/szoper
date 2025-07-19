package com.szopper.data.sync.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDataTransfer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val SERVICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        private const val SERVICE_NAME = "SzopperSync"
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    
    suspend fun startServer(): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions()) return@withContext false
        
        try {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME,
                SERVICE_UUID
            )
            true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun waitForClient(): BluetoothSocket? = withContext(Dispatchers.IO) {
        try {
            serverSocket?.accept()
        } catch (e: IOException) {
            null
        }
    }
    
    suspend fun connectToDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermissions()) return@withContext false
        
        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            clientSocket = device?.createRfcommSocketToServiceRecord(SERVICE_UUID)
            
            // Cancel discovery to improve connection performance
            bluetoothAdapter?.cancelDiscovery()
            
            clientSocket?.connect()
            clientSocket?.isConnected == true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun sendData(socket: BluetoothSocket, data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
            writer.write(data)
            writer.write("\n") // Add delimiter
            writer.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun receiveData(socket: BluetoothSocket): String? = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            reader.readLine()
        } catch (e: IOException) {
            null
        }
    }
    
    suspend fun performHandshake(socket: BluetoothSocket, isServer: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isServer) {
                // Server sends greeting first
                sendData(socket, "SZOPPER_SYNC_BT_SERVER")
                val clientResponse = receiveData(socket)
                clientResponse == "SZOPPER_SYNC_BT_CLIENT"
            } else {
                // Client responds to server greeting
                val serverGreeting = receiveData(socket)
                if (serverGreeting == "SZOPPER_SYNC_BT_SERVER") {
                    sendData(socket, "SZOPPER_SYNC_BT_CLIENT")
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun closeConnections() {
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            // Ignore close errors
        } finally {
            clientSocket = null
            serverSocket = null
        }
    }
    
    fun getCurrentClientSocket(): BluetoothSocket? = clientSocket
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}