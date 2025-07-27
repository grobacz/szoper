package com.szopper.domain.sync

data class DeviceInfo(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isAvailable: Boolean = true
)

enum class DeviceType {
    WIFI_DIRECT,
    BLUETOOTH
}
