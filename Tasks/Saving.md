### User requirements

As an application user I want to be able to save my shopping list in it's current state, and be able to restore it back to that state on demand.

I want to have a button that lets me either save the current state of database, including categories and products, with all their properties, under a name of my choice.
If the list alreaty have a name, it should be autofilled in the name field.
There should be a button under the main menu to restore saved state. After clicking the button, a list of saved states should be displayed. I should be able to choose one, and the application should return to main screen with the state restored to chosen version.

Apart from choosing a version to restore, I should have an option to remove the currently loaded version from the list of saved versions.

### Implementation plan

#### 1. Database Changes
- Create a new table `saved_states` to store:
  - `id`: Primary key (auto-increment)
  - `name`: String (user-provided name for the saved state)
  - `timestamp`: Long (when the state was saved)
  - `data`: BLOB (serialized JSON containing all categories and products)

#### 2. Data Model
- Create a new `SavedState` entity class
- Create a new `SavedStateDao` with methods for:
  - `getAll()`: Get all saved states
  - `getById(id)`: Get a specific saved state
  - `insert(state)`: Save a new state
  - `delete(state)`: Remove a saved state

#### 3. UI Components
1. **Save Current State Dialog**
   - Add a "Save Current List" option to the main menu
   - Show a dialog with:
     - An EditText pre-filled with the current list name (if any)
     - Save and Cancel buttons

2. **Restore State Dialog**
   - Add a "Restore Saved List" option to the main menu
   - Show a dialog with:
     - A RecyclerView listing all saved states (name and date)
     - Each item has:
       - State name
       - Save date
       - Delete button
     - A "Restore" button that's enabled when an item is selected

#### 4. Business Logic
1. **Saving State**
   - When user clicks "Save" in the save dialog:
     - Get all categories and products from the database
     - Serialize them to JSON
     - Save to the `saved_states` table with the provided name and current timestamp

2. **Restoring State**
   - When user selects a state and clicks "Restore":
     - Clear existing categories and products
     - Deserialize the selected state's data
     - Insert all categories and products into the database
     - Refresh the UI

3. **Deleting State**
   - When user clicks delete on a saved state:
     - Show confirmation dialog
     - If confirmed, remove from database
     - Update the list of saved states

#### 5. Error Handling
- Handle cases where:
  - No name is provided when saving
  - A state with the same name already exists
  - Database operations fail
  - Corrupted saved state data

#### 6. Testing
- Test saving and restoring with:
  - Empty lists
  - Large lists
  - Special characters in names
  - Offline mode
  - App restart after save/restore

#### 7. UI/UX Considerations
- Add visual feedback during save/restore operations
- Show loading indicators for long operations
- Add confirmation dialogs for destructive actions
- Ensure proper navigation flow

#### 8. Performance
- Run database operations in background threads
- Implement pagination for large lists of saved states
- Optimize JSON serialization/deserialization

#### 9. Future Enhancements
- Add ability to export/import saved states
- Add search functionality for saved states
- Add tags or categories to saved states
- Add ability to compare different saved states

