# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Szopper** is an Android shopping list application built with Java and Android SDK. The app supports categorized shopping lists, drag-and-drop functionality, Bluetooth synchronization between devices, and state save/restore functionality.

## Development Commands

### Build & Install
```bash
# Build and install debug version to connected device/emulator
./gradlew installDebug

# Build APK
./gradlew assembleDebug
```

### Emulator Commands
```bash
# Start preferred emulator
emulator -avd pixel4-android14

# Alternative emulator with optimizations
emulator -avd Test_Device_API_36 -no-boot-anim -gpu auto -no-audio
```

### Static Analysis & Quality
```bash
# Run all static analysis tools (lint, detekt, checkstyle, spotbugs)
./gradlew staticAnalysis

# Run static analysis without compilation (detekt, checkstyle only)
./gradlew staticAnalysisNoCompile

# Auto-fix formatting and style issues
./gradlew staticAnalysisFix

# Individual tools
./gradlew detekt
./gradlew checkstyle
./gradlew lintDebug
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

## Architecture Overview

### Database Layer (Room)
- **AppDatabase**: Main Room database with migration support (currently version 9)
- **Entities**: ProductEntity, CategoryEntity, SavedState
- **DAOs**: ProductDao, CategoryDao, SavedStateDao
- **Singleton Pattern**: AppDatabaseSingleton manages database instance lifecycle

### Core Components
- **MainActivity**: Primary activity with tab-based category navigation, product management, and Bluetooth sync
- **CategoryActivity**: Category management interface
- **ProductAdapter**: RecyclerView adapter with drag-and-drop, swipe-to-delete/check functionality

### Key Features
1. **Category Management**: Drag-reorderable tabs for different product categories
2. **Product Management**: Add, delete, check/uncheck items with position tracking
3. **Bluetooth Sync**: Device discovery and bidirectional data synchronization
4. **State Management**: Save/restore entire list states with serialization
5. **Drag & Drop**: ItemTouchHelper-based reordering for both categories and products

### Data Flow
- UI Layer (Activities/Fragments) → ViewModels → Repository/DAOs → Room Database
- Background operations use Executors.newSingleThreadExecutor()
- UI updates via runOnUiThread() callbacks

### Bluetooth Architecture
- Server socket listening on UUID: `00001101-0000-1000-8000-00805F9B34FB`
- SyncData class for bidirectional synchronization
- Conflict resolution based on lastModified timestamps

## Code Conventions

### Database Operations
- All database operations run on background threads via Executors
- UI updates happen on main thread via runOnUiThread()
- Use entities for database operations, domain models (Product) for UI

### Permissions
- Dynamic permission handling for Bluetooth (API level dependent)
- Location permissions required for device discovery on newer Android versions

### State Management
- Position-based ordering for both categories and products
- lastModified timestamps for sync conflict resolution
- Serialization support for complete state backup/restore

## Project Structure
```
app/src/main/java/com/grobacz/shoppinglistapp/
├── Activities: MainActivity, CategoryActivity
├── adapter/: RecyclerView adapters
├── dao/: Room Data Access Objects
├── model/: Entity and domain model classes
├── repository/: Data repository pattern implementations
├── ui/: Dialog fragments and custom UI components
├── utils/: Utility classes (Bluetooth, File, Network, etc.)
└── viewmodel/: ViewModels for MVVM pattern
```

## Important Notes

- Database migrations are carefully handled - never skip migration steps
- Bluetooth functionality requires proper permission handling across Android versions
- Static analysis is configured with Detekt, Checkstyle, SpotBugs, and Android Lint
- Position-based ordering is critical for drag-and-drop functionality
- Always test Bluetooth sync with actual devices, not just emulators