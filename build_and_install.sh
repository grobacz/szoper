#!/bin/bash

# Szopper App Build and Install Script
# This script builds the debug APK and installs it on a connected Android device

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "build.gradle" ] && [ ! -f "build.gradle.kts" ]; then
    print_error "Not in an Android project directory. Please run from the project root."
    exit 1
fi

print_status "Starting Szopper app build and install process..."

# Function to select target device
select_device() {
    local devices=()
    local device_names=()
    
    # Get list of connected devices (excluding header and empty lines)
    local adb_output
    adb_output=$(adb devices)
    
    while IFS= read -r line; do
        # Skip empty lines and header
        if [[ $line =~ ^[[:space:]]*$ ]] || [[ $line == "List of devices attached" ]]; then
            continue
        fi
        
        # Check if line contains 'device' status (more flexible matching)
        if [[ $line == *"device"* ]] && [[ ! $line == *"offline"* ]] && [[ ! $line == *"unauthorized"* ]]; then
            device_id=$(echo "$line" | awk '{print $1}')
            
            # Skip if device_id is empty
            if [[ -n "$device_id" ]]; then
                devices+=("$device_id")
            fi
        fi
    done <<< "$adb_output"
    
    # Get device names in a separate loop to avoid stdin conflicts
    for device_id in "${devices[@]}"; do
        device_model=$(adb -s "$device_id" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
        device_names+=("$device_model")
    done
    
    if [ ${#devices[@]} -eq 0 ]; then
        print_error "No Android devices connected via ADB!"
        print_status "Please connect your Android device and enable USB debugging."
        print_status "Then run: adb devices"
        exit 1
    elif [ ${#devices[@]} -eq 1 ]; then
        SELECTED_DEVICE="${devices[0]}"
        print_success "Found 1 connected device: ${device_names[0]} (${devices[0]})"
    else
        print_status "Multiple devices detected. Please choose:"
        echo
        for i in "${!devices[@]}"; do
            echo "  $((i+1)). ${device_names[i]} (${devices[i]})"
        done
        echo
        
        while true; do
            read -p "Enter device number (1-${#devices[@]}): " choice
            if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le ${#devices[@]} ]; then
                SELECTED_DEVICE="${devices[$((choice-1))]}"
                print_success "Selected: ${device_names[$((choice-1))]} (${devices[$((choice-1))]})"
                break
            else
                print_error "Invalid selection. Please enter a number between 1 and ${#devices[@]}."
            fi
        done
    fi
}

# Check for connected devices and select target
print_status "Checking for connected Android devices..."
select_device

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

# Run tests first
print_status "Running unit tests..."
if ./gradlew app:test; then
    print_success "All unit tests passed!"
else
    print_error "Unit tests failed!"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Build debug APK
print_status "Building debug APK..."
if ./gradlew app:assembleDebug; then
    print_success "APK built successfully!"
else
    print_error "Failed to build APK!"
    exit 1
fi

# Find the APK file
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    print_error "APK file not found at expected location: $APK_PATH"
    exit 1
fi

# Get APK info
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
print_status "APK size: $APK_SIZE"

# Install APK
print_status "Installing APK on selected device..."
if adb -s "$SELECTED_DEVICE" install -r "$APK_PATH"; then
    print_success "App installed successfully!"
else
    print_error "Failed to install APK!"
    print_status "Trying to uninstall and reinstall..."
    
    # Try to uninstall first, then install
    adb -s "$SELECTED_DEVICE" uninstall com.szopper.debug 2>/dev/null || true
    if adb -s "$SELECTED_DEVICE" install "$APK_PATH"; then
        print_success "App installed successfully after uninstall!"
    else
        print_error "Installation failed even after uninstall!"
        exit 1
    fi
fi

# Get device info
DEVICE_MODEL=$(adb -s "$SELECTED_DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Unknown")
ANDROID_VERSION=$(adb -s "$SELECTED_DEVICE" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || echo "Unknown")

print_success "Installation completed!"
print_status "Device: $DEVICE_MODEL (Android $ANDROID_VERSION)"
print_status "Package: com.szopper.debug"
print_status "APK: $APK_PATH ($APK_SIZE)"

# Optional: Launch the app
read -p "Launch the app now? (Y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    print_status "Launching Szopper app..."
    adb -s "$SELECTED_DEVICE" shell am start -n com.szopper.debug/com.szopper.presentation.MainActivity
    print_success "App launched!"
fi

print_success "Build and install process completed successfully!"
print_status "You can now test the drag and drop functionality on your device."