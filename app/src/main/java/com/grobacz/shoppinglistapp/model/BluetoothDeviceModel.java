package com.grobacz.shoppinglistapp.model;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceModel {
    private String name;
    private String address;
    private BluetoothDevice device;

    public BluetoothDeviceModel(BluetoothDevice device) {
        this.device = device;
        this.name = device.getName();
        this.address = device.getAddress();
    }

    public String getName() {
        return name != null ? name : "Unknown Device";
    }

    public String getAddress() {
        return address;
    }

    public BluetoothDevice getDevice() {
        return device;
    }
}
