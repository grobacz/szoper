package com.szopper.domain.sync

data class DeviceInfo(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isAvailable: Boolean = true,
    val hasSzopperApp: Boolean = true, // Assume true for service-discovered devices
    val signalStrength: Int = -1, // Optional signal strength indicator
    val discoveryMethod: DiscoveryMethod = DiscoveryMethod.SERVICE // How this device was discovered
)

enum class DeviceType {
    WIFI_DIRECT
}

enum class DiscoveryMethod {
    SERVICE,    // Discovered via DNS-SD service discovery
    PEER,       // Discovered via generic WiFi Direct peer discovery
    MANUAL      // Manually added by user
}
