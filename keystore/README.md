# Keystore Configuration

This directory contains the signing configuration for release builds.

## Release Signing Setup

### Generate Release Keystore

```bash
keytool -genkey -v -keystore release.keystore -alias szopper_release -keyalg RSA -keysize 2048 -validity 10000
```

### Environment Variables

Set these environment variables for automated builds:

```bash
export KEYSTORE_FILE="/path/to/keystore/release.keystore"
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="szopper_release"
export KEY_PASSWORD="your_key_password"
```

### Gradle Properties (Alternative)

Add to `~/.gradle/gradle.properties` or `keystore/gradle.properties`:

```properties
KEYSTORE_FILE=/path/to/keystore/release.keystore
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=szopper_release
KEY_PASSWORD=your_key_password
```

## Security Notes

- **Never commit keystore files to version control**
- **Keep passwords secure and never hardcode them**
- **Use environment variables or secure property files**
- **Backup keystore files securely**
- **Consider using Google Play App Signing for production**

## Build Commands

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Signed APK
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```