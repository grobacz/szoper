# Szopper - Installation Instructions

## System Requirements

### Minimum Requirements
- **Android Version**: 7.0 (API Level 24) or higher
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 100MB free space
- **Permissions**: Location, Nearby devices, Storage access

### Recommended Requirements
- **Android Version**: 10.0 (API Level 29) or higher
- **RAM**: 4GB or more
- **Storage**: 500MB free space
- **Network**: WiFi Direct and Bluetooth support

## Installation Methods

### Method 1: Direct APK Installation (Recommended)

#### Step 1: Download APK
1. Download the latest `szopper-release.apk` from the releases
2. Transfer the APK file to your Android device
3. Locate the APK file using a file manager

#### Step 2: Enable Unknown Sources
1. Open **Settings** on your Android device
2. Navigate to **Security** or **Privacy & Security**
3. Enable **"Install unknown apps"** or **"Unknown sources"**
4. Select your file manager app and allow installation

#### Step 3: Install APK
1. Tap on the `szopper-release.apk` file
2. Tap **"Install"** when prompted
3. Wait for installation to complete
4. Tap **"Open"** to launch Szopper

### Method 2: ADB Installation (Developer Method)

#### Prerequisites
- Android Debug Bridge (ADB) installed on your computer
- USB debugging enabled on your Android device
- USB cable to connect device to computer

#### Installation Steps
```bash
# Connect your device via USB
adb devices

# Install the APK
adb install szopper-release.apk

# Launch the app
adb shell am start -n com.szopper/.presentation.MainActivity
```

### Method 3: Android Studio (Development)

#### Prerequisites
- Android Studio installed
- Android SDK with API 24+ 
- Device or emulator running Android 7.0+

#### Build and Install
```bash
# Clone the repository
git clone <repository-url>
cd Szopper

# Build debug version
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install release version
./gradlew assembleRelease
./gradlew installRelease
```

## First-Time Setup

### 1. Grant Permissions
When you first open Szopper, you'll be asked to grant several permissions:

#### Required Permissions
- **Nearby devices**: For device discovery and connection
- **Location**: Required for WiFi Direct functionality (Android requirement)
- **Storage**: For database and app data storage

#### Permission Setup
1. Tap **"Allow"** when prompted for each permission
2. If you accidentally deny permissions, go to:
   - **Settings** > **Apps** > **Szopper** > **Permissions**
   - Enable all required permissions

### 2. Test Core Functionality
1. **Add a product**: Tap the "+" button and add "Test Item"
2. **Mark as bought**: Tap the checkbox next to "Test Item"
3. **Reset products**: Tap the refresh icon in the toolbar
4. **Navigation**: Tap the sync icon to access device synchronization

### 3. Test Synchronization (Optional)
If you have two devices:
1. Install Szopper on both devices
2. Open the sync screen on both devices
3. Wait for devices to appear in the list
4. Tap to connect and test data synchronization

## Troubleshooting Installation

### Common Issues

#### APK Installation Fails
**Problem**: "App not installed" error
**Solutions**:
- Ensure you have enough storage space (100MB minimum)
- Check that unknown sources are enabled
- Try restarting your device
- Download the APK again (file may be corrupted)

#### Permission Denied Errors
**Problem**: App crashes or features don't work
**Solutions**:
- Go to Settings > Apps > Szopper > Permissions
- Enable all required permissions manually
- Restart the app after granting permissions

#### WiFi Direct Not Working
**Problem**: Can't discover other devices
**Solutions**:
- Ensure location services are enabled
- Check that WiFi is turned on (even if not connected)
- Try enabling/disabling WiFi Direct in Android settings
- Move devices closer together (within 10 feet)

#### Bluetooth Sync Issues
**Problem**: Bluetooth fallback not working
**Solutions**:
- Ensure Bluetooth is enabled on both devices
- Make devices discoverable
- Clear Bluetooth cache: Settings > Apps > Bluetooth > Storage > Clear Cache
- Try pairing devices in Android settings first

### Compatibility Issues

#### Older Android Versions (7.0-8.1)
- Some WiFi Direct features may be limited
- Bluetooth sync should work reliably
- Performance may be slower on devices with less than 3GB RAM

#### Custom Android ROMs
- Some features may not work on heavily modified Android versions
- LineageOS, AOSP-based ROMs should work fine
- MIUI, OneUI, ColorOS generally compatible

#### Manufacturer-Specific Issues
- **Samsung**: May need to disable battery optimization for Szopper
- **Huawei**: Check that WiFi Direct is enabled in WiFi settings
- **Xiaomi**: Allow background activity for reliable sync
- **OnePlus**: Disable aggressive battery management

## Uninstallation

### Method 1: Android Settings
1. Go to **Settings** > **Apps** or **Application Manager**
2. Find and select **Szopper**
3. Tap **"Uninstall"**
4. Confirm by tapping **"OK"**

### Method 2: App Drawer
1. Long-press the **Szopper** app icon
2. Drag to **"Uninstall"** or tap the uninstall option
3. Confirm the uninstallation

### Method 3: ADB (Developer)
```bash
adb uninstall com.szopper
```

## Data Backup and Migration

### Manual Backup
Since Szopper stores data locally:
1. Use the sync feature to transfer data to another device
2. Data is automatically backed up during synchronization
3. No cloud backup is performed (privacy feature)

### Migration Between Devices
1. Install Szopper on the new device
2. Use the sync feature to transfer data from the old device
3. Verify all products are transferred correctly
4. Uninstall from the old device if desired

## Update Instructions

### APK Updates
1. Download the new APK version
2. Install over the existing version (data will be preserved)
3. Grant any new permissions if prompted
4. Test functionality after update

### Automatic Updates
Currently, Szopper doesn't support automatic updates. Check for new versions manually and follow the APK installation process.

## Support and Help

### Getting Help
- Check the **USER_GUIDE.md** for usage instructions
- Review **TECHNICAL_DOCUMENTATION.md** for architecture details
- Look at **ARCHITECTURE_DECISIONS.md** for design rationale

### Reporting Issues
If you encounter installation problems:
1. **Device Information**: Model, Android version, available storage
2. **Error Details**: Screenshot of error messages
3. **Steps to Reproduce**: What you were doing when the error occurred
4. **Logs**: Use `adb logcat` if you have developer tools

### Performance Optimization
After installation:
- **Battery Optimization**: Disable for Szopper to ensure reliable sync
- **Storage Management**: Keep at least 200MB free for optimal performance
- **Background Apps**: Close unnecessary apps during sync operations

---

## Quick Start Checklist

- [ ] Download Szopper APK
- [ ] Enable unknown sources in Android settings
- [ ] Install APK and grant all permissions
- [ ] Add your first product to test functionality
- [ ] Test sync with another device (if available)
- [ ] Review user guide for advanced features

**Congratulations!** Szopper is now installed and ready to make your shopping experience better! 🛒✨