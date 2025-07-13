package com.grobacz.shoppinglistapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Utility class for network operations.
 */
public class NetworkUtils {

    private static final int CONNECTION_TIMEOUT_MS = 3000; // 3 seconds
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Checks if the device is connected to the internet.
     * @param context The context
     * @return True if the device is connected to the internet, false otherwise
     */
    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && 
                  (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Deprecated in API 29
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }
    
    /**
     * Checks if the device is connected to a Wi-Fi network.
     * @param context The context
     * @return True if connected to Wi-Fi, false otherwise
     */
    public static boolean isWifiConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            // Deprecated in API 29
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && 
                   activeNetwork.isConnected() && 
                   activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }
    }
    
    /**
     * Checks if the device is connected to a mobile network.
     * @param context The context
     * @return True if connected to mobile data, false otherwise
     */
    public static boolean isMobileDataConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            // Deprecated in API 29
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && 
                   activeNetwork.isConnected() && 
                   activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }
    
    /**
     * Checks if the device has an active internet connection by pinging a server.
     * This is a more reliable check than just checking for network connectivity.
     * @return True if the device can reach the internet, false otherwise
     */
    public static boolean hasInternetAccess() {
        try {
            // Try to connect to a known reliable server
            Future<Boolean> future = executor.submit(new InternetCheckCallable());
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Callable to check internet access by pinging a server.
     */
    private static class InternetCheckCallable implements Callable<Boolean> {
        @Override
        public Boolean call() {
            try {
                // Try to connect to Google's DNS server
                int timeoutMs = 1500;
                Socket socket = new Socket();
                InetSocketAddress socketAddress = new InetSocketAddress("8.8.8.8", 53);
                socket.connect(socketAddress, timeoutMs);
                socket.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
    
    /**
     * Checks if a URL is reachable.
     * @param urlString The URL to check
     * @param timeoutMs The timeout in milliseconds
     * @return True if the URL is reachable, false otherwise
     */
    public static boolean isUrlReachable(String urlString, int timeoutMs) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the network type as a string.
     * @param context The context
     * @return A string representing the network type (e.g., "Wi-Fi", "Mobile", "Ethernet", "Unknown")
     */
    public static String getNetworkType(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return "Unknown";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return "Disconnected";
            }
            
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "Unknown";
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mobile";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "VPN";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return "Bluetooth";
            } else {
                return "Unknown";
            }
        } else {
            // Deprecated in API 29
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
                return "Disconnected";
            }
            
            int type = activeNetwork.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    return "Wi-Fi";
                case ConnectivityManager.TYPE_MOBILE:
                    return "Mobile";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "Ethernet";
                case ConnectivityManager.TYPE_VPN:
                    return "VPN";
                case ConnectivityManager.TYPE_BLUETOOTH:
                    return "Bluetooth";
                default:
                    return "Unknown";
            }
        }
    }
    
    /**
     * Gets the network signal strength in dBm.
     * Note: This requires location permission on Android 9 (API 28) and above.
     * @param context The context
     * @return The signal strength in dBm, or Integer.MIN_VALUE if unknown
     */
    public static int getSignalStrength(@NonNull Context context) {
        // This is a simplified implementation
        // In a real app, you would use TelephonyManager or WifiManager to get the actual signal strength
        return Integer.MIN_VALUE; // Unknown signal strength
    }
    
    /**
     * Gets the network download speed in Mbps.
     * @param context The context
     * @return The download speed in Mbps, or -1 if unknown
     */
    public static double getDownloadSpeedMbps(@NonNull Context context) {
        // This is a placeholder implementation
        // In a real app, you would perform a speed test to measure the actual download speed
        return -1; // Unknown speed
    }
    
    /**
     * Gets the network upload speed in Mbps.
     * @param context The context
     * @return The upload speed in Mbps, or -1 if unknown
     */
    public static double getUploadSpeedMbps(@NonNull Context context) {
        // This is a placeholder implementation
        // In a real app, you would perform a speed test to measure the actual upload speed
        return -1; // Unknown speed
    }
}
