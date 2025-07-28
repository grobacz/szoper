#!/bin/bash

# Quick build and install script (skips tests)
# Usage: ./quick_install.sh

set -e

echo "🔧 Building APK..."
./gradlew app:assembleDebug

echo "📱 Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "🚀 Launching app..."
adb shell am start -n com.szopper.debug/com.szopper.presentation.MainActivity

echo "✅ Done! App installed and launched successfully!"
echo ""
echo "📌 Improvements made:"
echo "   • Dragged item now follows finger position accurately"
echo "   • Improved drag sensitivity (60% threshold)"
echo "   • Better visual feedback with scale and elevation"
echo "   • Smoother drag animations"