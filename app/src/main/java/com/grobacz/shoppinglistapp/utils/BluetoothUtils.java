package com.grobacz.shoppinglistapp.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.grobacz.shoppinglistapp.model.BluetoothDeviceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for Bluetooth operations.
 */
public class BluetoothUtils {

    // Request codes
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_DISCOVERABLE = 2;
    
    private static final long SCAN_PERIOD = 10000; // 10 seconds
    
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BluetoothScanCallback scanCallback;
    private android.bluetooth.le.ScanCallback leScanCallback;
    private boolean isScanning = false;
    
    /**
     * Callback interface for Bluetooth scan results.
     */
    public interface BluetoothScanCallback {
        /**
         * Called when a new device is discovered.
         * @param device The discovered device
         */
        void onDeviceDiscovered(BluetoothDeviceModel device);
        
        /**
         * Called when the scan is complete.
         */
        void onScanComplete();
        
        /**
         * Called when there's an error during scanning.
         * @param errorMessage The error message
         */
        void onError(String errorMessage);
    }
    
    /**
     * Constructor.
     * @param context The context
     */
    public BluetoothUtils(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize Bluetooth adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
            this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        } else {
            // Deprecated in API 31
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }
    
    /**
     * Checks if the device supports Bluetooth.
     * @return True if the device supports Bluetooth, false otherwise
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }
    
    /**
     * Checks if Bluetooth is enabled.
     * @return True if Bluetooth is enabled, false otherwise
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * Prompts the user to enable Bluetooth.
     * @param activity The activity that should receive the result
     * @return True if the user was prompted, false if Bluetooth is already enabled
     */
    @SuppressLint("MissingPermission")
    public boolean enableBluetooth(Activity activity) {
        if (!isBluetoothSupported()) {
            return false;
        }
        
        if (!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return true;
        }
        
        return false;
    }
    
    /**
     * Makes the device discoverable to other Bluetooth devices.
     * @param activity The activity that should receive the result
     * @param duration The duration in seconds that the device should be discoverable
     */
    @SuppressLint("MissingPermission")
    public void makeDiscoverable(Activity activity, int duration) {
        if (!isBluetoothSupported()) {
            return;
        }
        
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        activity.startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }
    
    /**
     * Gets the list of paired devices.
     * @return A list of paired Bluetooth devices
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothDeviceModel> getPairedDevices() {
        List<BluetoothDeviceModel> devices = new ArrayList<>();
        
        if (!isBluetoothSupported() || !isBluetoothEnabled()) {
            return devices;
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        
        for (BluetoothDevice device : pairedDevices) {
            devices.add(new BluetoothDeviceModel(device));
        }
        
        return devices;
    }
    
    /**
     * Starts scanning for Bluetooth devices.
     * @param callback The callback to receive scan results
     * @return True if the scan was started, false otherwise
     */
    @RequiresPermission(allOf = {
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    })
    public boolean startScan(BluetoothScanCallback callback) {
        if (!isBluetoothSupported() || !isBluetoothEnabled()) {
            if (callback != null) {
                callback.onError("Bluetooth is not supported or not enabled");
            }
            return false;
        }
        
        // Stop any existing scan
        if (isScanning) {
            stopScan();
        }
        
        this.scanCallback = callback;
        this.isScanning = true;
        
        // Stop scanning after the defined scan period
        handler.postDelayed(this::stopScan, SCAN_PERIOD);
        
        // Start the scan
        boolean scanStarted = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startScanNew();
            } else {
                startScanLegacy();
            }
            scanStarted = true;
        } catch (Exception e) {
            isScanning = false;
            if (callback != null) {
                callback.onError("Failed to start scan: " + e.getMessage());
            }
        }
        
        return scanStarted;
    }
    
    /**
     * Stops scanning for Bluetooth devices.
     */
    public void stopScan() {
        if (!isScanning) {
            return;
        }
        
        // Stop the scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stopScanNew();
        } else {
            stopScanLegacy();
        }
        
        isScanning = false;
        
        // Notify the callback that scanning has stopped
        if (scanCallback != null) {
            scanCallback.onScanComplete();
            scanCallback = null;
        }
        
        // Remove any pending scan timeouts
        handler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Checks if the device is currently scanning for Bluetooth devices.
     * @return True if scanning, false otherwise
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    private void startScanNew() {
        // Implementation for Android 5.0 (API 21) and above
        leScanCallback = new android.bluetooth.le.ScanCallback() {
            @Override
            public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                super.onScanResult(callbackType, result);
                if (scanCallback != null) {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    
                    // Create a model for the device
                    BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(device);
                    deviceModel.setRssi(rssi);
                    
                    // Notify the callback
                    scanCallback.onDeviceDiscovered(deviceModel);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                if (scanCallback != null) {
                    scanCallback.onError("Scan failed with error code: " + errorCode);
                }
                isScanning = false;
            }
        };

        try {
            // Start the scan
            bluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
        } catch (Exception e) {
            if (scanCallback != null) {
                scanCallback.onError("Failed to start scan: " + e.getMessage());
            }
            isScanning = false;
        }
    }
    
    @SuppressWarnings("deprecation")
    @SuppressLint("MissingPermission")
    private void startScanLegacy() {
        // Implementation for Android 4.3 (API 18) to 4.4 (API 19)
        BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (scanCallback != null) {
                    // Create a model for the device
                    BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(device);
                    deviceModel.setRssi(rssi);
                    
                    // Notify the callback on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> 
                        scanCallback.onDeviceDiscovered(deviceModel)
                    );
                }
            }
        };
        
        // Start the BLE scan
        bluetoothAdapter.startLeScan(leScanCallback);
    }
    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    private void stopScanNew() {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null && leScanCallback != null) {
            try {
                bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
            } catch (Exception e) {
                if (scanCallback != null) {
                    scanCallback.onError("Failed to stop scan: " + e.getMessage());
                }
            } finally {
                leScanCallback = null;
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    @SuppressLint("MissingPermission")
    private void stopScanLegacy() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) scanCallback);
        }
    }
    
    /**
     * Connects to a Bluetooth device.
     * @param deviceModel The device to connect to
     * @return True if the connection was initiated, false otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean connectToDevice(@NonNull BluetoothDeviceModel deviceModel) {
        // Implementation would depend on the type of Bluetooth device and profile
        // This is a placeholder for the actual implementation
        return false;
    }
    
    /**
     * Disconnects from a Bluetooth device.
     * @param deviceModel The device to disconnect from
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnectDevice(@NonNull BluetoothDeviceModel deviceModel) {
        // Implementation would depend on the type of Bluetooth device and profile
        // This is a placeholder for the actual implementation
    }
    
    /**
     * Gets the connection state of a Bluetooth device.
     * @param deviceModel The device to check
     * @return The connection state, or -1 if the device is not connected
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public int getConnectionState(@NonNull BluetoothDeviceModel deviceModel) {
        // Implementation would depend on the type of Bluetooth device and profile
        // This is a placeholder for the actual implementation
        return -1;
    }
    
    /**
     * Sends data to a connected Bluetooth device.
     * @param deviceModel The device to send data to
     * @param data The data to send
     * @return True if the data was sent, false otherwise
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public boolean sendData(@NonNull BluetoothDeviceModel deviceModel, @NonNull byte[] data) {
        // Implementation would depend on the type of Bluetooth device and profile
        // This is a placeholder for the actual implementation
        return false;
    }
    
    /**
     * Releases resources used by the BluetoothUtils instance.
     */
    public void cleanup() {
        stopScan();
        scanCallback = null;
        handler.removeCallbacksAndMessages(null);
    }
}
