# New feature requests:
1. allow drag and drop on the list, to be able to re-order the items. Item position on the list should become part of it's properties, and be accounted for during synchronization
2. add option in settings to disable haptic feedback on UI clicks. when disabled, list item clicks (marking and unmarking), adding item and other buttons should not cause vibrations

# Implementation plans:

## Feature 1: Drag and drop re-ordering
- [ ] **Domain Layer:**
    - [ ] Update the `Product` data class in `app/src/main/java/com/szopper/domain/model/Product.kt` to include a `position` field.
- [ ] **Data Layer:**
    - [ ] Update `ProductRepository` and `ProductRepositoryImpl` to handle the new `position` field.
    - [ ] Update `RealmDatabase` to include the `position` in the Realm model.
    - [ ] Update the sync mechanism to include the `position` field in the data synchronization.
- [ ] **Presentation Layer (UI):**
    - [ ] Modify `ProductListScreen.kt` to implement drag-and-drop functionality.
    - [ ] Update the `ProductListViewModel` to handle the reordering logic and persist changes.
- [ ] **Testing:**
    - [ ] Create a `ReorderProductUseCaseTest.kt`.
    - [ ] Update `ProductRepositoryImplTest.kt` to test the `position` field.
    - [ ] Update `ProductListViewModelTest.kt` to test reordering.
    - [ ] Update `ProductListScreenTest.kt` to test the drag and drop UI.

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
