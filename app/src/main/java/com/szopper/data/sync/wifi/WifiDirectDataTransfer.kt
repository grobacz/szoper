package com.szopper.data.sync.wifi

import android.net.wifi.p2p.WifiP2pInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectDataTransfer @Inject constructor() {
    
    companion object {
        private const val PORT = 8888
        private const val SOCKET_TIMEOUT = 10000 // 10 seconds
    }
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    
    suspend fun startServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(PORT)
            serverSocket?.soTimeout = SOCKET_TIMEOUT
            true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun waitForClient(): Socket? = withContext(Dispatchers.IO) {
        try {
            serverSocket?.accept()
        } catch (e: IOException) {
            null
        }
    }
    
    suspend fun connectToServer(hostAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            clientSocket = Socket()
            clientSocket?.connect(InetSocketAddress(hostAddress, PORT), SOCKET_TIMEOUT)
            clientSocket?.isConnected == true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun sendData(socket: Socket, data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.write(data)
            writer.write("\n") // Add delimiter
            writer.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    suspend fun receiveData(socket: Socket): String? = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            reader.readLine()
        } catch (e: IOException) {
            null
        }
    }
    
    suspend fun establishConnection(connectionInfo: WifiP2pInfo): Socket? = withContext(Dispatchers.IO) {
        if (connectionInfo.groupFormed) {
            if (connectionInfo.isGroupOwner) {
                // This device is the group owner (server)
                if (startServer()) {
                    waitForClient()
                } else {
                    null
                }
            } else {
                // This device is a client
                val hostAddress = connectionInfo.groupOwnerAddress.hostAddress
                if (hostAddress != null && connectToServer(hostAddress)) {
                    clientSocket
                } else {
                    null
                }
            }
        } else {
            null
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
    
    suspend fun performHandshake(socket: Socket, isServer: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isServer) {
                // Server sends greeting first
                sendData(socket, "SZOPPER_SYNC_SERVER")
                val clientResponse = receiveData(socket)
                clientResponse == "SZOPPER_SYNC_CLIENT"
            } else {
                // Client responds to server greeting
                val serverGreeting = receiveData(socket)
                if (serverGreeting == "SZOPPER_SYNC_SERVER") {
                    sendData(socket, "SZOPPER_SYNC_CLIENT")
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}