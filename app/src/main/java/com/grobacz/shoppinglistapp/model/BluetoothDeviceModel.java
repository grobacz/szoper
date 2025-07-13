package com.grobacz.shoppinglistapp.model;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class BluetoothDeviceModel implements Parcelable {
    private String name;
    private String address;
    private BluetoothDevice device;
    private int rssi;

    @SuppressLint("MissingPermission")
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

    public int getRssi() {
        return rssi;
    }
    
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeParcelable(device, flags);
        dest.writeInt(rssi);
    }

    protected BluetoothDeviceModel(Parcel in) {
        name = in.readString();
        address = in.readString();
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        rssi = in.readInt();
    }

    public static final Creator<BluetoothDeviceModel> CREATOR = new Creator<BluetoothDeviceModel>() {
        @Override
        public BluetoothDeviceModel createFromParcel(Parcel in) {
            return new BluetoothDeviceModel(in);
        }

        @Override
        public BluetoothDeviceModel[] newArray(int size) {
            return new BluetoothDeviceModel[size];
        }
    };
}
