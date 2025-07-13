package com.grobacz.shoppinglistapp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling runtime permissions.
 */
public class PermissionUtils {

    // Permission request codes
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST_BLUETOOTH = 101;
    public static final int PERMISSION_REQUEST_LOCATION = 102;
    public static final int PERMISSION_REQUEST_STORAGE = 103;
    public static final int PERMISSION_REQUEST_CAMERA = 104;
    
    // Permission groups
    public static final String[] BLUETOOTH_PERMISSIONS;
    public static final String[] LOCATION_PERMISSIONS;
    public static final String[] STORAGE_PERMISSIONS;
    public static final String[] CAMERA_PERMISSIONS;
    
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            BLUETOOTH_PERMISSIONS = new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-11 (API 29-30)
            BLUETOOTH_PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            // Android 9 and below (API 28-)
            BLUETOOTH_PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        
        LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9.0 (API 28) and below
            STORAGE_PERMISSIONS = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            // Android 10-11 (API 29-30)
            STORAGE_PERMISSIONS = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            };
        } else {
            // Android 12+ (API 31+)
            STORAGE_PERMISSIONS = new String[]{
                // No permissions needed for app-specific storage
            };
        }
        
        CAMERA_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }
    
    /**
     * Checks if all the requested permissions are granted.
     * @param context The context
     * @param permissions The permissions to check
     * @return True if all permissions are granted, false otherwise
     */
    public static boolean hasPermissions(Context context, String[] permissions) {
        if (context == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Requests permissions from an Activity.
     * @param activity The activity
     * @param permissions The permissions to request
     * @param requestCode The request code
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (activity == null || permissions == null || permissions.length == 0) {
            return;
        }
        
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Check which permissions we need to request
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toArray(new String[0]),
                requestCode
            );
        }
    }
    
    /**
     * Requests permissions from a Fragment.
     * @param fragment The fragment
     * @param permissions The permissions to request
     * @param requestCode The request code
     */
    public static void requestPermissions(Fragment fragment, String[] permissions, int requestCode) {
        if (fragment.getContext() == null || permissions == null || permissions.length == 0) {
            return;
        }
        
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Check which permissions we need to request
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            fragment.requestPermissions(
                permissionsToRequest.toArray(new String[0]),
                requestCode
            );
        }
    }
    
    /**
     * Checks if all the requested permissions were granted.
     * @param grantResults The grant results from onRequestPermissionsResult
     * @return True if all permissions were granted, false otherwise
     */
    public static boolean verifyPermissions(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }
        
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if we should show the permission rationale.
     * @param activity The activity
     * @param permission The permission to check
     * @return True if we should show the rationale, false otherwise
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        if (activity == null) {
            return false;
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
    
    /**
     * Checks if we should show the permission rationale.
     * @param fragment The fragment
     * @param permission The permission to check
     * @return True if we should show the rationale, false otherwise
     */
    public static boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
        if (fragment.getActivity() == null) {
            return false;
        }
        return fragment.shouldShowRequestPermissionRationale(permission);
    }
    
    /**
     * Checks if the user has permanently denied a permission.
     * @param activity The activity
     * @param permission The permission to check
     * @return True if the user has permanently denied the permission, false otherwise
     */
    public static boolean isPermissionPermanentlyDenied(Activity activity, String permission) {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
               ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Checks if the user has permanently denied a permission.
     * @param fragment The fragment
     * @param permission The permission to check
     * @return True if the user has permanently denied the permission, false otherwise
     */
    public static boolean isPermissionPermanentlyDenied(Fragment fragment, String permission) {
        return !fragment.shouldShowRequestPermissionRationale(permission) &&
               ContextCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Gets the permission group name for a permission.
     * @param permission The permission
     * @return The permission group name, or null if not found
     */
    public static String getPermissionGroup(String permission) {
        if (permission == null) {
            return null;
        }
        
        if (permission.startsWith("android.permission-group.")) {
            return permission;
        }
        
        // Map individual permissions to their groups
        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
            permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            permission.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            return "android.permission-group.STORAGE";
        } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                  permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                  permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            return "android.permission-group.LOCATION";
        } else if (permission.equals(Manifest.permission.CAMERA)) {
            return "android.permission-group.CAMERA";
        } else if (permission.startsWith("android.permission.BLUETOOTH")) {
            return "android.permission-group.BLUETOOTH";
        } else if (permission.startsWith("android.permission-group.")) {
            return permission;
        }
        
        return null;
    }
}
