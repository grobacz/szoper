## Feature 4: Fix and enhance device-to-device synchronization functionality
The app has synchronization of data feature via wifi-direct. It parially works, there is a menu for it and permission handling.
The discovery limits devices found to only ones that has the app installed. This seems not to work, there are no devices found despite there bing another device with the app on it, and active.
The synchronization itself is untested on real devices, we cannot tell if it works and if it is fully implemented. The intention was for it to synchonize the database of the app between two devices, two way, based on latest update of product item between devices.

## Analysis and Implementation Plan

### Current State Analysis

#### 🔍 WiFi Direct Discovery Issues
1. **Service Registration**: `WifiDirectManager` properly registers Szopper service with `SERVICE_TYPE = "_szopper._tcp"` and `SERVICE_NAME = "Szopper"`
2. **Service Discovery**: Uses DNS-SD service discovery to find only Szopper app instances
3. **Filtering Logic**: Correctly filters devices to show only those with the Szopper service registered
4. **Permission Handling**: Properly checks for `ACCESS_FINE_LOCATION` permission

**Root Cause**: The discovery mechanism is working correctly in theory, but there are several potential issues:
- WiFi Direct requires both devices to be on the same WiFi network or in direct P2P mode
- Service registration timing - services may not be immediately discoverable
- Android platform limitations with DNS-SD service discovery reliability

#### 🔄 Synchronization Protocol Status
1. **Protocol Design**: Complete sync protocol with handshake, data transfer, and conflict resolution
2. **Data Transfer**: Implemented socket communication for WiFi Direct
3. **Conflict Resolution**: Strategy-based resolution with timestamps
4. **Two-way Sync**: `performFullSync()` method handles bidirectional synchronization

**Status**: Theoretically complete but untested on real hardware

### 🛠️ Proposed Solutions

#### Phase 1: Fix Device Discovery (High Priority)
1. **Enhanced Logging**: Add comprehensive debug logging for discovery process
2. **Fallback Discovery**: Implement generic P2P peer discovery alongside service discovery
3. **Service Registration Retry**: Add retry mechanism for service registration
4. **Discovery Timeout**: Implement proper discovery timeouts and restart mechanisms
5. **Network State Monitoring**: Check WiFi P2P availability before starting discovery

#### Phase 2: Improve Discovery Reliability (High Priority)
1. **Hybrid Discovery Approach**:
   - Use both DNS-SD service discovery AND generic peer discovery
   - Cross-reference discovered peers with service information
   - Show all WiFi Direct peers with indication of Szopper app presence
2. **Manual Connection Option**: Allow manual connection by device MAC address
3. **Discovery State Management**: Better handling of discovery lifecycle

#### Phase 3: Real Device Testing Protocol (High Priority)
1. **Test Environment Setup**:
   - Two Android devices with the app installed
   - WiFi Direct capabilities enabled on both devices
   - Location permissions granted
2. **Testing Scenarios**:
   - Device A starts discovery, Device B registers service
   - Device B starts discovery, Device A registers service
   - Both devices discover each other simultaneously
   - Connection establishment and handshake
   - Data synchronization in both directions
   - Conflict resolution with overlapping changes

#### Phase 4: Synchronization Validation (Medium Priority)
1. **Data Integrity Testing**:
   - Verify all products sync correctly
   - Test conflict resolution scenarios
   - Validate timestamp-based merging
2. **Edge Case Handling**:
   - Network disconnection during sync
   - Large product lists synchronization
   - Rapid successive changes

#### Phase 5: Enhanced Error Handling (Medium Priority)
1. **User Feedback**: Clear error messages for common issues
2. **Automatic Retry**: Smart retry mechanisms for failed operations
3. **Fallback Options**: Bluetooth fallback when WiFi Direct fails
4. **Connection Recovery**: Automatic reconnection attempts

### 🔧 Specific Code Changes Required

#### 1. Enhanced WifiDirectManager Discovery
```kotlin
// Add fallback peer discovery
fun discoverPeersWithFallback(): Flow<List<DeviceInfo>> = callbackFlow {
    // Start service discovery (current implementation)
    val serviceDevices = mutableMapMap<String, DeviceInfo>()
    val peerDevices = mutableMap<String, DeviceInfo>()
    
    // Add generic peer discovery as fallback
    manager?.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            manager?.requestPeers(wifiP2pChannel) { peers ->
                peers.deviceList.forEach { device ->
                    if (!serviceDevices.containsKey(device.deviceAddress)) {
                        // Mark as potential Szopper device
                        val deviceInfo = DeviceInfo(
                            id = device.deviceAddress,
                            name = device.deviceName ?: "Unknown Device",
                            type = DeviceType.WIFI_DIRECT,
                            isAvailable = device.status == WifiP2pDevice.AVAILABLE,
                            hasSzopperApp = false // Will be determined later
                        )
                        peerDevices[device.deviceAddress] = deviceInfo
                    }
                }
                trySend((serviceDevices.values + peerDevices.values).toList())
            }
        }
        override fun onFailure(reason: Int) { /* Handle failure */ }
    })
}
```

#### 2. Enhanced DeviceInfo Model
```kotlin
data class DeviceInfo(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isAvailable: Boolean,
    val hasSzopperApp: Boolean = true, // Assume true for service-discovered devices
    val signalStrength: Int = -1 // Optional signal strength indicator
)
```

#### 3. Improved Connection Testing
```kotlin
// Add connection validation
suspend fun validateConnection(socket: Socket): Boolean {
    return try {
        // Send ping message
        val pingMessage = syncProtocolHandler.createPingMessage(getDeviceId())
        val success = wifiDirectDataTransfer.sendData(socket, 
            syncProtocolHandler.serializeMessage(pingMessage))
        
        if (success) {
            // Wait for pong response with timeout
            withTimeout(5000) {
                val response = wifiDirectDataTransfer.receiveData(socket)
                val message = syncProtocolHandler.deserializeMessage(response ?: "")
                message?.type == MessageType.PONG
            }
        } else false
    } catch (e: Exception) {
        false
    }
}
```

### 📋 Testing Checklist

#### Discovery Testing
- [ ] Service registration succeeds on both devices
- [ ] Service discovery finds registered devices within 30 seconds
- [ ] Fallback peer discovery shows non-Szopper devices
- [ ] Manual connection by MAC address works
- [ ] Discovery restart after timeout works

#### Connection Testing  
- [ ] WiFi Direct connection establishment succeeds
- [ ] Socket handshake completes successfully
- [ ] Connection validation ping/pong works
- [ ] Connection survives brief network interruptions
- [ ] Proper cleanup on connection failure

#### Synchronization Testing
- [ ] Single product sync works both directions
- [ ] Multiple products sync correctly
- [ ] Conflict resolution merges changes properly
- [ ] Large product lists sync without timeout
- [ ] Sync works after app restart

#### Error Handling Testing
- [ ] Clear error messages for permission denials
- [ ] Graceful handling of network disconnections
- [ ] Proper error reporting for sync failures
- [ ] Recovery from partial sync failures

### 🎯 Success Criteria
1. **Discovery**: Both devices find each other within 30 seconds in 80% of attempts
2. **Connection**: Successful connection establishment in 90% of discovery attempts
3. **Sync**: Complete bidirectional sync within 10 seconds for typical product lists
4. **Reliability**: Sync success rate of 95% for stable connections
5. **User Experience**: Clear status indicators and helpful error messages

### 📈 Implementation Priority
1. **Immediate (Week 1)**: Enhanced discovery logging and fallback mechanisms
2. **High (Week 2)**: Real device testing environment and basic connectivity
3. **Medium (Week 3)**: Synchronization validation and edge case handling
4. **Low (Week 4)**: UI improvements and advanced error handling

