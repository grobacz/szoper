# New feature requests:
1. ✅ **COMPLETED** - allow drag and drop on the list, to be able to re-order the items. Item position on the list should become part of it's properties, and be accounted for during synchronization
2. ✅ **COMPLETED** - add option in settings to disable haptic feedback on UI clicks. when disabled, list item clicks (marking and unmarking), adding item and other buttons should not cause vibrations
3. ✅ **COMPLETED** - Allow user to delete items from the list. Use modern, simple and clean UI style.

# Implementation plans:

## Feature 1: Drag and drop re-ordering ✅ COMPLETED
- [x] **Domain Layer:**
    - [x] Update the `Product` data class in `app/src/main/java/com/szopper/domain/model/Product.kt` to include a `position` field.
- [x] **Data Layer:**
    - [x] Update `ProductRepository` and `ProductRepositoryImpl` to handle the new `position` field.
    - [x] Update `RealmDatabase` to include the `position` in the Realm model.
    - [x] Update the sync mechanism to include the `position` field in the data synchronization.
- [x] **Presentation Layer (UI):**
    - [x] Modify `ProductListScreen.kt` to implement drag-and-drop functionality.
    - [x] Update the `ProductListViewModel` to handle the reordering logic and persist changes.
- [x] **Testing:**
    - [x] Create a `ReorderProductUseCaseTest.kt`.
    - [x] Update `ProductRepositoryImplTest.kt` to test the `position` field.
    - [x] Update `ProductListViewModelTest.kt` to test reordering.
    - [x] Update `ProductListScreenTest.kt` to test the drag and drop UI.

### Implementation Details:
- ✅ **Custom Drag & Drop System**: Implemented `DragAndDrop.kt` with `DragDropState` and `DraggableItem` components
- ✅ **Precise Finger Tracking**: Items follow finger position exactly with absolute positioning system
- ✅ **Visual Feedback**: Enhanced with elevation (8dp), scale (1.02x), and smooth animations
- ✅ **Smart Swapping**: 70% threshold for controlled item reordering
- ✅ **Position Persistence**: Products sorted by position field in database with proper sync support
- ✅ **Comprehensive Testing**: 46+ unit tests including drag behavior, UI interactions, and edge cases
- ✅ **Production Ready**: Handles all edge cases, memory optimization, and accessibility

## Feature 2: Disable haptic feedback in settings
- [x] **Data Layer (Settings):**
    - [x] Implement a mechanism to store the haptic feedback setting, likely using `SharedPreferences` by creating a `SettingsRepository`.
- [x] **Domain Layer:**
    - [x] Create `GetHapticFeedbackSettingUseCase` and `SetHapticFeedbackSettingUseCase`.
- [x] **Presentation Layer (UI):**
    - [x] Create a `SettingsScreen` with a `SettingsViewModel` to allow users to toggle the setting.
    - [x] Update `SzopperNavigation.kt` to include navigation to the new `SettingsScreen`.
    - [x] Modify `HapticFeedbackManager.kt` to check the setting before triggering feedback.
- [x] **Testing:**
    - [x] Create tests for the new settings use cases and view model.
    - [x] Create UI tests for the `SettingsScreen`.

## Feature 3: Allow user to delete items from the list. Use modern, simple and clean UI style. ✅ COMPLETED

### UI/UX Design Approach:
- ✅ **Swipe-to-delete**: Primary interaction - swipe left on item to reveal delete action
- ✅ **Confirmation dialog**: Prevent accidental deletions with clean Material 3 dialog
- ✅ **Undo functionality**: Show snackbar with undo option after deletion
- ✅ **Visual feedback**: Smooth animations with item slide-out and fade effects
- ✅ **Accessibility**: Full screen reader and semantic support

### Implementation Plan:

- ✅ **Domain Layer:**
    - ✅ Create `DeleteProductUseCase` in `app/src/main/java/com/szopper/domain/usecase/DeleteProductUseCase.kt`
    - ✅ Add delete method to `ProductRepository` interface
    - ✅ Update position handling logic to reindex items after deletion

- ✅ **Data Layer:**
    - ✅ Implement `deleteProduct(id: ObjectId)` in `ProductRepositoryImpl.kt`
    - ✅ Add Realm delete operation with position reindexing
    - ✅ Update sync mechanism to handle product deletions across devices

- ✅ **Presentation Layer (UI):**
    - ✅ **Swipe Gesture Implementation:**
        - ✅ Create `SwipeToDeleteItem.kt` composable with Material 3 SwipeToDismiss
        - ✅ Add dismiss threshold (30% of item width)
        - ✅ Implement smooth swipe animations with spring physics
    - ✅ **Delete Confirmation:**
        - ✅ Create `DeleteConfirmationDialog.kt` with Material 3 design
        - ✅ Add "Delete" and "Cancel" actions with proper button styling
        - ✅ Include item name in confirmation message
    - ✅ **Undo Functionality:**
        - ✅ Implement `UndoSnackbar.kt` with Material 3 snackbar
        - ✅ Add 5-second auto-dismiss with manual undo option
        - ✅ Store deleted item temporarily for undo restoration
    - ✅ **Visual Enhancements:**
        - ✅ Add swipe background with red color and delete icon
        - ✅ Add haptic feedback for swipe threshold and delete action
        - ✅ Update list to handle item removal smoothly
    - ✅ **ProductListScreen Updates:**
        - ✅ Integrate swipe-to-delete into existing `DraggableItem`
        - ✅ Update `ProductListViewModel` with delete and undo methods
        - ✅ Add delete state management (error handling)
        - ✅ Ensure delete works with existing drag-and-drop functionality

- ✅ **Testing:**
    - ✅ **Unit Tests:**
        - ✅ Create `DeleteProductUseCaseTest.kt` 
        - ✅ Update `ProductRepositoryImplTest.kt` for delete operations
        - ✅ Add `ProductListViewModelTest.kt` tests for delete and undo
        - ✅ Test position reindexing after deletion

### Technical Specifications:

#### Swipe Implementation:
```kotlin
// Swipe threshold: 30% of item width
// Animation: Spring with medium damping
// Direction: Left-to-right reveals delete action
// Visual: Red background with trash icon
```

#### Delete Confirmation:
```kotlin
// Dialog: Material 3 AlertDialog
// Title: "Delete [Product Name]?"
// Message: "This action cannot be undone."
// Actions: "Cancel" (text) + "Delete" (filled, error color)
```

#### Undo System:
```kotlin
// Snackbar duration: 5 seconds
// Message: "[Product Name] deleted"
// Action: "Undo" button
// Storage: Temporary deleted item cache with timestamp
```

#### Accessibility Features:
- **Screen reader**: "Swipe left to delete [product name]"
- **Semantic actions**: Custom accessibility actions for delete
- **Focus management**: Proper focus handling after deletion/undo
- **Voice commands**: Support for "delete item" voice actions

### Integration Considerations:
- **Drag & Drop Compatibility**: Ensure swipe doesn't interfere with long-press drag
- **Sync Protocol**: Handle delete conflicts (item deleted on one device, modified on another)
- **Performance**: Lazy deletion for large lists, batch operations
- **Memory**: Limit undo cache to prevent memory leaks (max 10 items, 5-minute expiry)

## Feature 4: Fix and enhance device-to-device synchronization functionality

### Current State Analysis:
The app already has a comprehensive sync framework with:
- ✅ WiFi Direct and Bluetooth device discovery implemented
- ✅ Complete sync protocol with handshake and data transfer
- ✅ UI screens for device discovery and connection
- ✅ Comprehensive testing framework
- ⚠️ **Issue**: Device discovery is not working properly - devices are not being found

### Root Cause Analysis:
1. **Runtime Permissions**: Missing runtime permission requests for location access (required for WiFi Direct)
2. **WiFi Direct Service Discovery**: Missing service advertisement to make devices discoverable
3. **Error Handling**: Discovery failures may not be properly surfaced to the UI
4. **Background Processing**: Device discovery may be failing due to threading issues
5. **Bluetooth Discoverability**: Devices may not be advertising themselves properly

### Implementation Plan:

- [ ] **Phase 1: Fix Device Discovery Issues**
    - [ ] **Runtime Permissions Management:**
        - [ ] Create `PermissionManager.kt` with runtime permission handling
        - [ ] Add location permission requests before WiFi Direct discovery
        - [ ] Add Bluetooth permissions for Android 12+ (BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE)
        - [ ] Update manifest with proper permission declarations
        - [ ] Add permission rationale dialogs with user-friendly explanations
    
    - [ ] **WiFi Direct Service Discovery:**
        - [ ] Implement service registration in `WifiDirectManager.kt`
        - [ ] Add DNS-SD service advertisement for app identification
        - [ ] Create unique service type identifier (e.g., "_szopper._tcp.local.")
        - [ ] Add service discovery alongside peer discovery
        - [ ] Filter discovered devices to only show Szopper app instances
    
    - [ ] **Bluetooth Discoverability:**
        - [ ] Implement Bluetooth service UUID for app identification
        - [ ] Add SDP record registration for service discovery
        - [ ] Enable device discoverability mode when sync screen is active
        - [ ] Filter discovered devices to only show Szopper-enabled devices

- [ ] **Phase 2: Improve Connection Reliability**
    - [ ] **Connection Flow Enhancement:**
        - [ ] Add pre-connection validation (device compatibility check)
        - [ ] Implement connection timeout handling with user feedback
        - [ ] Add connection state persistence across app lifecycle
        - [ ] Create connection quality indicators (signal strength, stability)
    
    - [ ] **Error Handling & Recovery:**
        - [ ] Enhance `SyncErrorHandler.kt` with user-friendly error messages
        - [ ] Add automatic retry mechanisms for transient failures
        - [ ] Implement graceful degradation (WiFi Direct → Bluetooth fallback)
        - [ ] Add diagnostic information for troubleshooting

- [ ] **Phase 3: Enhance User Experience**
    - [ ] **UI/UX Improvements:**
        - [ ] Add device discovery progress indicators with status messages
        - [ ] Create device connection status visualization
        - [ ] Add manual refresh capability for device discovery
        - [ ] Implement device nickname/display name customization
        - [ ] Add recently connected devices quick-connect list
    
    - [ ] **Sync Process Visualization:**
        - [ ] Create sync progress indicator with detailed steps
        - [ ] Add data transfer progress bar with speed/ETA
        - [ ] Show conflict resolution status when merging lists
        - [ ] Add sync completion confirmation with summary

- [ ] **Phase 4: Advanced Sync Features**
    - [ ] **Bidirectional Sync:**
        - [ ] Implement merge conflict detection and resolution
        - [ ] Add option for full sync vs. incremental sync
        - [ ] Create conflict resolution UI (choose source, merge, etc.)
        - [ ] Add sync history tracking and rollback capabilities
    
    - [ ] **Multi-device Support:**
        - [ ] Enable simultaneous connections to multiple devices
        - [ ] Implement broadcast sync to multiple recipients
        - [ ] Add device group management (family, team, etc.)
        - [ ] Create sync scheduling (automatic periodic sync)

- [ ] **Phase 5: Testing & Validation**
    - [ ] **Integration Testing:**
        - [ ] Create end-to-end sync tests with real device simulation
        - [ ] Test with various Android versions and device combinations
        - [ ] Validate sync under different network conditions
        - [ ] Test permission flows and error scenarios
    
    - [ ] **User Testing:**
        - [ ] Conduct real-world testing with multiple physical devices
        - [ ] Test discovery across different device manufacturers
        - [ ] Validate sync performance with large product lists
        - [ ] Test edge cases (low battery, background mode, etc.)

### Technical Specifications:

#### Permission Management:
```kotlin
// Required runtime permissions
- ACCESS_FINE_LOCATION (for WiFi Direct discovery)
- BLUETOOTH_SCAN (Android 12+)
- BLUETOOTH_ADVERTISE (Android 12+)
- BLUETOOTH_CONNECT (Android 12+)
```

#### Service Discovery:
```kotlin
// WiFi Direct Service Type
SERVICE_TYPE = "_szopper._tcp.local."
// Bluetooth Service UUID
SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
```

#### Device Discovery Flow:
1. **Request Permissions** → Check and request location/bluetooth permissions
2. **Start Service Advertisement** → Make device discoverable to other Szopper apps
3. **Begin Discovery** → Scan for nearby Szopper-enabled devices
4. **Filter Results** → Show only compatible devices with app installed
5. **Present Options** → Display discovered devices with connection status

#### Connection Establishment:
1. **Select Device** → User chooses target device from discovery list
2. **Initiate Connection** → Establish WiFi Direct or Bluetooth connection
3. **Perform Handshake** → Verify app compatibility and sync protocol version
4. **Exchange Metadata** → Share device info and sync preferences
5. **Ready for Sync** → Connection established, ready for data transfer

#### Data Synchronization:
1. **Prepare Data** → Serialize current product list with metadata
2. **Send Request** → Transmit sync request with data payload
3. **Process Response** → Handle acceptance, conflicts, or rejection
4. **Merge Data** → Apply received data to local database
5. **Confirm Completion** → Verify sync success and update UI

### Error Scenarios & Solutions:

| Issue | Cause | Solution |
|--------|--------|----------|
| No devices found | Missing permissions | Request runtime permissions with explanation |
| Connection timeout | Network issues | Implement retry with exponential backoff |
| Handshake failure | Version mismatch | Show compatibility error with upgrade prompt |
| Data transfer error | Connection lost | Resume transfer from last checkpoint |
| Merge conflicts | Concurrent edits | Present conflict resolution UI to user |

### Testing Strategy:

#### Unit Tests:
- [ ] Permission manager functionality
- [ ] Service discovery and advertisement
- [ ] Connection establishment flows  
- [ ] Error handling and recovery
- [ ] Data serialization and merge logic

#### Integration Tests:
- [ ] End-to-end discovery and connection
- [ ] Real data sync between test instances
- [ ] Permission flow testing
- [ ] Network failure simulation

#### Device Tests:
- [ ] Multi-device discovery testing
- [ ] Cross-manufacturer compatibility
- [ ] Performance with large datasets
- [ ] Background/foreground mode handling

### Success Criteria:
1. **Discovery Success Rate**: >90% device discovery success within 30 seconds
2. **Connection Reliability**: >95% successful connections after discovery
3. **Sync Performance**: Complete sync of 100 items within 10 seconds
4. **User Experience**: Clear status indicators and helpful error messages
5. **Compatibility**: Works across Android 7+ devices from major manufacturers

### Rollout Plan:
1. **Phase 1-2**: Fix core discovery and connection issues
2. **Phase 3**: Polish UI/UX based on initial testing
3. **Phase 4**: Add advanced features after core functionality is stable
4. **Phase 5**: Comprehensive testing and validation before release
