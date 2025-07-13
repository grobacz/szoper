package com.grobacz.shoppinglistapp;

// Android imports
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

// AndroidX imports
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

// Material Design imports
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

// App imports
import com.grobacz.shoppinglistapp.adapter.DeviceListAdapter;
import com.grobacz.shoppinglistapp.adapter.ProductAdapter;
import com.grobacz.shoppinglistapp.adapter.SavedStateAdapter;
import com.grobacz.shoppinglistapp.dao.CategoryDao;
import com.grobacz.shoppinglistapp.dao.ProductDao;
import com.grobacz.shoppinglistapp.model.BluetoothDeviceModel;
import com.grobacz.shoppinglistapp.model.CategoryEntity;
import com.grobacz.shoppinglistapp.model.ProductEntity;
import com.grobacz.shoppinglistapp.model.SavedState;
import com.grobacz.shoppinglistapp.ui.RestoreStateDialogFragment;
import com.grobacz.shoppinglistapp.ui.SaveStateDialogFragment;
import com.grobacz.shoppinglistapp.utils.ShoppingListState;
import com.grobacz.shoppinglistapp.utils.StateSerializer;
import com.grobacz.shoppinglistapp.viewmodel.SavedStateViewModel;

// Java imports
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    // Interface moved to separate file

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ArrayList<Product> productList;
    private ExtendedFloatingActionButton fabAdd;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private boolean isDiscovering = false;
    private DeviceListAdapter deviceListAdapter;
    private android.app.AlertDialog deviceListDialog;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Permission request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final long SCAN_PERIOD = 10000; // 10 seconds
    
    // Permissions arrays
    private static final String[] REQUIRED_PERMISSIONS;
    
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS = new String[]{
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS = new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            REQUIRED_PERMISSIONS = new String[0];
        }
    }

    private AppDatabase db;
    private com.grobacz.shoppinglistapp.dao.ProductDao productDao;
    private com.grobacz.shoppinglistapp.dao.CategoryDao categoryDao;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private java.util.List<CategoryEntity> categories;
    private int selectedCategoryId = -1;
    private SavedStateViewModel savedStateViewModel;
    private String currentListName = "";

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d("MainActivity", "onTabSelected called");
        Object tag = tab.getTag();
        if (tag != null) {
            selectedCategoryId = (int) tag;
            Log.d("MainActivity", "Tab selected, categoryId: " + selectedCategoryId);
            loadProductsForCategory(selectedCategoryId);
        } else {
            Log.d("MainActivity", "Tab has no tag");
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        // Not used
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // Not used
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        // Check database state
        AppDatabase.checkDatabase(this);
        // Force reload categories and tabs
        runOnUiThread(this::loadCategoriesAndTabs);
        
        // Recheck permissions when returning to the app
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    
    /**
     * Checks if all required permissions are granted and requests them if not
     * @return true if all permissions are already granted, false otherwise
     */
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // No runtime permissions needed before Android 6.0
        }
        
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toArray(new String[0]),
                REQUEST_PERMISSIONS
            );
            return false;
        }
        
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Bluetooth permissions are required for device discovery and connection. Core app functionality will work without them.", Toast.LENGTH_LONG).show();
            } else {
                // Permissions granted, try to start Bluetooth server if not already started
                if (checkBluetoothState()) {
                    startBluetoothServer();
                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Method to reset the database for testing
    public static void resetDatabase(android.content.Context context) {
        android.util.Log.d("AppDatabase", "Resetting database...");
        context.deleteDatabase("shopping_list_database");
        // Reset the singleton instance
        AppDatabaseSingleton.resetInstance();
        android.util.Log.d("AppDatabase", "Database reset complete");
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database using singleton
        db = AppDatabaseSingleton.getInstance(this);
        productDao = db.productDao();
        categoryDao = db.categoryDao();
        
        // Create default categories if database is empty
        createDefaultCategoriesIfNeeded();
        
        // Initialize ViewModel
        savedStateViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication())).get(SavedStateViewModel.class);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tabLayout = findViewById(R.id.tabLayout);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Initialize views first (before Bluetooth setup)
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        Log.d("MainActivity", "FAB found: " + (fabAdd != null ? "YES" : "NULL"));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList, this::updateProductPositions);
        recyclerView.setAdapter(adapter);
        
        // Add divider between items
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        // Set up FAB click listener
        Log.d("MainActivity", "Setting up FAB click listener");
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Log.d("MainActivity", "FAB clicked! selectedCategoryId: " + selectedCategoryId);
                showAddProductDialog();
            });
            Log.d("MainActivity", "FAB click listener set successfully");
        } else {
            Log.e("MainActivity", "FAB is null, cannot set click listener");
        }

        // Load categories and set up tab selection
        // Set up tab layout
        tabLayout.addOnTabSelectedListener(this);

        // Set up bottom bar button click listeners
        findViewById(R.id.btnCategories).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnSync).setOnClickListener(v -> startDeviceDiscovery());
        
        // Initialize Bluetooth after UI setup
        initializeBluetoothAfterUI();
    }

    private void initializeBluetoothAfterUI() {
        // Check if device supports Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Request runtime permissions (non-blocking)
        checkAndRequestPermissions();
        
        // Ensure Bluetooth is enabled (non-blocking)
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        // Start Bluetooth server if possible
        if (checkBluetoothState()) {
            startBluetoothServer();
        }
    }

    private void showSaveStateDialog() {
        // Create a dialog with an input field
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Current State");
        
        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("Enter a name for this state");
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                saveState(name);
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    // Note: Menu functionality is handled by the toolbar menu in the layout
    
    private void saveState(String name) {
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Saving state...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Run in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current categories and products
                List<com.grobacz.shoppinglistapp.model.CategoryEntity> categories = categoryDao.getAllSync();
                List<com.grobacz.shoppinglistapp.model.ProductEntity> products = productDao.getAllProducts();
                
                // Serialize the data
                byte[] data = StateSerializer.serializeState(categories, products);
                
                // Create and save the state
                SavedState state = new SavedState(name, System.currentTimeMillis(), data);
                
                // Insert the state on the main thread since Room handles background execution
                runOnUiThread(() -> {
                    savedStateViewModel.insert(state);
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "State saved successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to save state: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void showRestoreStateDialog() {
        RestoreStateDialogFragment dialog = RestoreStateDialogFragment.newInstance();
        dialog.setRestoreStateListener(new RestoreStateDialogFragment.OnStateRestoreListener() {
            @Override
            public void onStateRestored(com.grobacz.shoppinglistapp.model.SavedState state) {
                restoreState(state);
            }
        });
        dialog.show(getSupportFragmentManager(), "restore_state_dialog");
    }
    
    private void restoreState(com.grobacz.shoppinglistapp.model.SavedState state) {
        if (state == null) return;
        
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Restoring state...");
        progressDialog.setCancelable(false);
        
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore State")
                .setMessage("Are you sure you want to restore state: " + state.getName() + "? This will replace your current list.")
                .setPositiveButton("Restore", (dialog, which) -> {
                    progressDialog.show();
                    
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            // Deserialize the data
                            ShoppingListState listState = StateSerializer.deserializeState(state.getData());
                            
                            if (listState != null && listState.getCategories() != null && listState.getProducts() != null) {
                                // Clear existing data
                                List<ProductEntity> allProducts = productDao.getAllProducts();
                                for (ProductEntity product : allProducts) {
                                    productDao.delete(product);
                                }
                                
                                List<CategoryEntity> allCategories = categoryDao.getAllSync();
                                for (CategoryEntity category : allCategories) {
                                    categoryDao.delete(category);
                                }
                                
                                // Insert the restored categories
                                for (CategoryEntity category : listState.getCategories()) {
                                    categoryDao.insert(category);
                                }
                                
                                // Insert the restored products
                                for (ProductEntity product : listState.getProducts()) {
                                    productDao.insert(product);
                                }
                                
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    // Update UI
                                    loadCategoriesAndTabs();
                                    Toast.makeText(MainActivity.this, "State restored successfully", 
                                        Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                throw new IOException("Invalid state data");
                            }
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                String errorMessage = "Failed to restore state: " + 
                                    (e.getMessage() != null ? e.getMessage() : "Unknown error");
                                Toast.makeText(MainActivity.this, errorMessage, 
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> progressDialog.dismiss())
                .show();
    }
    
    private void startDeviceDiscovery() {
        if (!checkBluetoothState()) return;

        // Create and show the device list dialog
        showDeviceListDialog();

        // Start discovery
        startDiscovery();
    }

    private boolean checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showDeviceListDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_device_list, null);
        RecyclerView deviceList = dialogView.findViewById(R.id.deviceList);
        View progressBar = dialogView.findViewById(R.id.progressBar);
        TextView statusMessage = dialogView.findViewById(R.id.statusMessage);
        View btnScan = dialogView.findViewById(R.id.btnScan);

        deviceList.setLayoutManager(new LinearLayoutManager(this));
        deviceListAdapter = new DeviceListAdapter(device -> {
            // Handle device selection
            connectToDevice(device);
            if (deviceListDialog != null && deviceListDialog.isShowing()) {
                deviceListDialog.dismiss();
            }
        });
        deviceList.setAdapter(deviceListAdapter);

        btnScan.setOnClickListener(v -> startDiscovery());

        deviceListDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Select a device")
                .setView(dialogView)
                .setNegativeButton("Cancel", (dialog, which) -> cancelDiscovery())
                .setOnDismissListener(dialog -> cancelDiscovery())
                .show();
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (isDiscovering) {
            cancelDiscovery();
        }

        if (!checkBluetoothState()) return;

        // Clear previous results
        if (deviceListAdapter != null) {
            deviceListAdapter.clearDevices();
        }

        // Start discovery
        isDiscovering = true;
        if (deviceListDialog != null) {
            View progressBar = deviceListDialog.findViewById(R.id.progressBar);
            TextView statusMessage = deviceListDialog.findViewById(R.id.statusMessage);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (statusMessage != null) statusMessage.setText("Searching for devices...");
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        // Start discovery
        if (!bluetoothAdapter.startDiscovery()) {
            Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show();
            cancelDiscovery();
        }

        // Automatically stop discovery after SCAN_PERIOD
        new Handler().postDelayed(this::cancelDiscovery, SCAN_PERIOD);
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    runOnUiThread(() -> {
                        if (deviceListAdapter != null) {
                            deviceListAdapter.addDevice(new BluetoothDeviceModel(device));
                        }
                    });
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished
                runOnUiThread(() -> {
                    isDiscovering = false;
                    if (deviceListDialog != null) {
                        View progressBar = deviceListDialog.findViewById(R.id.progressBar);
                        TextView statusMessage = deviceListDialog.findViewById(R.id.statusMessage);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (statusMessage != null) {
                            statusMessage.setText(deviceListAdapter.getItemCount() > 0 ? 
                                "Select a device to connect" : "No devices found");
                        }
                    }
                });
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void cancelDiscovery() {
        if (!isDiscovering) return;
        
        isDiscovering = false;
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
        
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        // TODO: Implement connection logic
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDiscovery();
    }



    // Start Bluetooth server thread
    private void startBluetoothServer() {
        new Thread(this::startServer).start();
    }

    private void showAddProductDialog() {
        Log.d("MainActivity", "showAddProductDialog called, selectedCategoryId: " + selectedCategoryId);
        if (selectedCategoryId == -1) {
            Log.d("MainActivity", "No category selected, showing toast");
            Toast.makeText(this, "Please select a category first", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("MainActivity", "Category selected, showing dialog");

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_product, null);
        dialog.setContentView(dialogView);
        Log.d("MainActivity", "Dialog created, about to show");

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnAdd);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();

            if (!productName.isEmpty()) {
                int quantity = 1; // Default quantity
                try {
                    if (!quantityStr.isEmpty()) {
                        quantity = Integer.parseInt(quantityStr);
                        if (quantity <= 0) quantity = 1;
                    }
                } catch (NumberFormatException e) {
                    quantity = 1;
                }

                // Get next position for the product in this category
                int nextPosition = 0;
                if (!productList.isEmpty()) {
                    nextPosition = productList.get(productList.size() - 1).getPosition() + 1;
                }

                // Create and add the product
                Product product = new Product(
                    productName,
                    quantity,
                    false, // isChecked
                    nextPosition,
                    selectedCategoryId,
                    System.currentTimeMillis()
                );

                productList.add(product);
                adapter.notifyItemInserted(productList.size() - 1);

                // Insert into database
                Executors.newSingleThreadExecutor().execute(() -> {
                    productDao.insert(product.toEntity());
                });

                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter a product name", Toast.LENGTH_SHORT).show();
            }
        });

        Log.d("MainActivity", "Calling dialog.show()");
        dialog.show();
        Log.d("MainActivity", "Dialog.show() called");
    }

    private void updateProductPositions() {
        long currentTime = System.currentTimeMillis();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i < adapter.products.size(); i++) {
                    Product product = adapter.products.get(i);
                    // Update the product's lastModified timestamp
                    product.setLastModified(currentTime);
                    product.setPosition(i);
                    
                    // Update in database using the product's ID
                    ProductEntity entity = product.toEntity();
                    if (entity.getId() > 0) { // Only update if it has a valid ID
                        productDao.update(entity);
                        Log.d("MainActivity", "Updated position for product ID " + entity.getId() + " to position " + i);
                    } else {
                        Log.w("MainActivity", "Product has no ID, cannot update position: " + product.getName());
                    }
                }
                
                // Update the UI
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error updating product positions", e);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startServer() {
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("ShoppingList", MY_UUID);
            while (true) {
                BluetoothSocket socket = serverSocket.accept();
                new Thread(new BluetoothHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    private void showSyncDialog() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            // Show device selection dialog
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(discoveryIntent, 2);
        }
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final ArrayList<Product> products;
        private final ItemTouchHelper itemTouchHelper;
        private final MainActivity activity;

        private final Runnable updatePositionsCallback;

        ProductAdapter(MainActivity activity, ArrayList<Product> products, Runnable updatePositionsCallback) {
            this.activity = activity;
            this.products = products;
            this.updatePositionsCallback = updatePositionsCallback;

            // Set up ItemTouchHelper for drag and drop
            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
            ) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                    int fromPosition = source.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();
                    
                    if (fromPosition != toPosition) {
                        // Move the item to the new position
                        Product movedProduct = products.remove(fromPosition);
                        products.add(toPosition, movedProduct);
                        
                        // Update positions for all affected products
                        for (int i = 0; i < products.size(); i++) {
                            products.get(i).setPosition(i);
                        }
                        
                        // Update positions in the database by running the callback on the UI thread
                        if (updatePositionsCallback != null) {
                            activity.runOnUiThread(updatePositionsCallback);
                        }

                        notifyItemMoved(fromPosition, toPosition);
                    }
                    return true;
                }

                @Override
                public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                    return 0.5f; // Swipe threshold at 50%
                }



                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    final int position = viewHolder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION || position >= products.size()) {
                        viewHolder.itemView.setTranslationX(0); // Reset position
                        return;
                    }
                    
                    final Product originalProduct = products.get(position);
                    
                    // Reset the view's position immediately to prevent it from being removed
                    viewHolder.itemView.post(() -> {
                        viewHolder.itemView.setTranslationX(0);
                        viewHolder.itemView.setAlpha(1);
                    });
                    
                    if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right - decrease quantity, cross out at 0, remove on next swipe
                        if (originalProduct.getQuantity() == 0) {
                            // Item is already at 0 quantity (crossed out), so remove it
                            activity.runOnUiThread(() -> {
                                if (position < products.size()) {
                                    Product removed = products.remove(position);
                                    notifyItemRemoved(position);
                                    
                                    // Delete from database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        activity.productDao.delete(removed.toEntity());
                                    });
                                }
                            });
                        } else {
                            // Decrease quantity (may reach 0, which will cross it out)
                            int newQuantity = Math.max(0, originalProduct.getQuantity() - 1);
                            final Product updated = new Product(
                                originalProduct.getName(),
                                newQuantity,
                                originalProduct.isChecked(),
                                originalProduct.getPosition(),
                                originalProduct.getCategoryId(),
                                System.currentTimeMillis()
                            );
                            updated.setId(originalProduct.getId()); // Preserve the original ID
                            
                            activity.runOnUiThread(() -> {
                                if (position < products.size()) {
                                    products.set(position, updated);
                                    notifyItemChanged(position);
                                    
                                    // Update in database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        try {
                                            ProductEntity entity = updated.toEntity();
                                            Log.d("MainActivity", "Updating product ID " + entity.getId() + " with quantity " + entity.getQuantity());
                                            activity.productDao.update(entity);
                                        } catch (Exception e) {
                                            Log.e("MainActivity", "Error updating product in database", e);
                                        }
                                    });
                                }
                            });
                        }
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left - increase quantity (minimum 1, so crossed-out items get restored)
                        int newQuantity = Math.max(1, originalProduct.getQuantity() + 1);
                        final Product updated = new Product(
                            originalProduct.getName(),
                            newQuantity,
                            originalProduct.isChecked(),
                            originalProduct.getPosition(),
                            originalProduct.getCategoryId(),
                            System.currentTimeMillis()
                        );
                        updated.setId(originalProduct.getId()); // Preserve the original ID
                        
                        activity.runOnUiThread(() -> {
                            if (position < products.size()) {
                                products.set(position, updated);
                                notifyItemChanged(position);
                                
                                // Update in database
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    try {
                                        ProductEntity entity = updated.toEntity();
                                        Log.d("MainActivity", "Updating product ID " + entity.getId() + " with quantity " + entity.getQuantity());
                                        activity.productDao.update(entity);
                                    } catch (Exception e) {
                                        Log.e("MainActivity", "Error updating product in database", e);
                                    }
                                });
                            }
                        });
                    }
                }

                @Override
                public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder.itemView.setAlpha(0.5f);
                    }
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    viewHolder.itemView.setAlpha(1.0f);
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return true;
                }
            };


            this.itemTouchHelper = new ItemTouchHelper(callback);
            this.itemTouchHelper.attachToRecyclerView(activity.recyclerView);
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.bind(product);

            // Set up drag handle
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder);
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return products.size();
        }
        
        public void onItemMove(int fromPosition, int toPosition) {
            if (fromPosition != toPosition) {
                // Move the item to the new position
                Product movedProduct = products.remove(fromPosition);
                products.add(toPosition, movedProduct);
                
                // Update positions for all affected products
                for (int i = 0; i < products.size(); i++) {
                    products.get(i).setPosition(i);
                }
                
                // Notify the adapter that the item has moved
                notifyItemMoved(fromPosition, toPosition);
                
                // Update positions in the database by running the callback on the UI thread
                if (updatePositionsCallback != null) {
                    activity.runOnUiThread(updatePositionsCallback);
                }
            }
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtProductName;
            private final TextView txtQuantity;
            private final ImageView dragHandle;

            ProductViewHolder(View itemView) {
                super(itemView);
                txtProductName = itemView.findViewById(R.id.txtProductName);
                txtQuantity = itemView.findViewById(R.id.quantityChip);
                dragHandle = itemView.findViewById(R.id.dragHandle);
            }

            void bind(Product product) {
                txtProductName.setText(product.getName());
                txtQuantity.setText(String.valueOf(product.getQuantity()));

                // Strikethrough if checked (crossed out when quantity reaches 0)
                if (product.isChecked() || product.getQuantity() == 0) {
                    txtProductName.setPaintFlags(txtProductName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    txtQuantity.setPaintFlags(txtQuantity.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    txtProductName.setPaintFlags(txtProductName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    txtQuantity.setPaintFlags(txtQuantity.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }
        }
    }


    private class BluetoothHandler implements Runnable {
        private final BluetoothSocket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;

        BluetoothHandler(BluetoothSocket socket) {
            this.socket = socket;
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // 1. Prepare data to send
                List<CategoryEntity> categoryList = new ArrayList<>();
                List<ProductEntity> productEntities = productDao.getAllProducts();
                List<Product> productList = new ArrayList<>();
                
                // Get categories synchronously for this operation
                categoryList = categoryDao.getAllSync();
                if (categoryList == null) {
                    categoryList = new ArrayList<>();
                }
                
                for (ProductEntity entity : productEntities) {
                    productList.add(Product.fromEntity(entity));
                }
                
                SyncData syncData = new SyncData(categoryList, productList);

                // 2. Send SyncData
                outputStream.writeObject(syncData);

                // 3. Receive SyncData
                SyncData receivedData = (SyncData) inputStream.readObject();

                // 4. Synchronize categories first
                Executors.newSingleThreadExecutor().execute(() -> {
                    for (CategoryEntity cat : receivedData.categories) {
                        if (categoryDao.getCategoryByName(cat.getName()) == null) {
                            categoryDao.insert(cat);
                        }
                    }
                    // 5. Synchronize products (with category relationship)
                    for (Product prod : receivedData.products) {
                        ProductEntity existingProduct = productDao.getByName(prod.getName());
                        // Only update if the received product is newer
                        if (existingProduct == null || existingProduct.getLastModified() < prod.getLastModified()) {
                            // Map category name to local categoryId if needed
                            CategoryEntity localCat = categoryDao.getByIdSync(prod.getCategoryId());
                            if (localCat != null) {
                                prod.setCategoryId(localCat.getId());
                            }
                            // Convert Product to ProductEntity before inserting/updating
                            ProductEntity prodEntity = prod.toEntity();
                            if (existingProduct != null) {
                                // Update existing product
                                prodEntity.setId(existingProduct.getId());
                                productDao.update(prodEntity);
                            } else {
                                // Insert new product
                                productDao.insert(prodEntity);
                            }
                        }
                    }
                    // 6. Update UI as needed
                    runOnUiThread(() -> {
                        loadProductsForCategory(selectedCategoryId);
                        adapter.notifyDataSetChanged();
                    });
                });
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }



    private void updateCategoryPositions() {
        if (categories == null) return;
        
        // Update positions in the list
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }
        
        // Update database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i < categories.size(); i++) {
                    CategoryEntity category = categories.get(i);
                    category.setPosition(i);
                    categoryDao.update(category);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // If there's an error, reload categories to ensure consistency
                runOnUiThread(this::loadCategoriesAndTabs);
            }
        });
    }

    private void moveCategory(int fromPosition, int toPosition) {
        if (categories == null || fromPosition < 0 || toPosition < 0 || 
            fromPosition >= categories.size() || toPosition >= categories.size() ||
            fromPosition == toPosition) {
            return;
        }

        // Update the list
        CategoryEntity category = categories.remove(fromPosition);
        categories.add(toPosition, category);

        // Update the UI
        tabLayout.removeTabAt(fromPosition);
        TabLayout.Tab newTab = tabLayout.newTab().setText(category.getName());
        newTab.setTag(category.getId());
        tabLayout.addTab(newTab, toPosition, false);
        
        // Select the moved tab
        tabLayout.selectTab(tabLayout.getTabAt(toPosition));
        selectedCategoryId = category.getId();
        loadProductsForCategory(selectedCategoryId);

        // Update positions in the database
        updateCategoryPositions();
        
        // Provide haptic feedback
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            tabLayout.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void loadCategoriesAndTabs() {       
        Log.d("MainActivity", "loadCategoriesAndTabs called");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {                
                // Get all categories, ordered by position
                categories = categoryDao.getAllSync();
                Log.d("MainActivity", "Found " + categories.size() + " categories");
                
                // Sort categories by position to ensure correct order
                Collections.sort(categories, (c1, c2) -> Integer.compare(c1.getPosition(), c2.getPosition()));
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    if (tabLayout == null) return;
                    
                    // Remove all tabs
                    tabLayout.removeAllTabs();
                    
                    // Add tabs in the correct order
                    for (CategoryEntity cat : categories) {
                        TabLayout.Tab tab = tabLayout.newTab().setText(cat.getName());
                        tab.setTag(cat.getId());
                        tabLayout.addTab(tab);
                    }
                    
                    // Set up drag and drop for categories
                    if (tabLayout instanceof CategoryTabLayout) {
                        ((CategoryTabLayout) tabLayout).setOnTabDragListener((fromPosition, toPosition) -> {
                            moveCategory(fromPosition, toPosition);
                        });
                    }
                    
                    // Select the first tab if none is selected
                    if (!categories.isEmpty() && tabLayout.getSelectedTabPosition() == -1) {
                        selectedCategoryId = categories.get(0).getId();
                        Log.d("MainActivity", "Auto-selecting first category: " + selectedCategoryId);
                        tabLayout.selectTab(tabLayout.getTabAt(0));
                        loadProductsForCategory(selectedCategoryId);
                    } else if (selectedCategoryId != -1) {
                        // Try to find and select the previously selected tab
                        for (int i = 0; i < categories.size(); i++) {
                            if (categories.get(i).getId() == selectedCategoryId) {
                                tabLayout.selectTab(tabLayout.getTabAt(i));
                                break;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // If there's an error, try to recover by resetting the category order
                runOnUiThread(this::resetCategoryOrder);
            }
        });
    }
    
    private void createDefaultCategoriesIfNeeded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Check if categories already exist
                List<CategoryEntity> existingCategories = categoryDao.getAllSync();
                if (existingCategories.isEmpty()) {
                    Log.d("MainActivity", "No categories found, creating default categories");
                    
                    // Create default categories
                    String[] defaultCategories = {
                        "Groceries", "Household", "Personal Care", "Electronics", "Clothing"
                    };
                    
                    for (int i = 0; i < defaultCategories.length; i++) {
                        CategoryEntity category = new CategoryEntity(defaultCategories[i]);
                        category.setPosition(i);
                        categoryDao.insert(category);
                    }
                    
                    // Reload categories and tabs on main thread
                    runOnUiThread(this::loadCategoriesAndTabs);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error creating default categories", e);
            }
        });
    }
    
    private void resetCategoryOrder() {
        if (categories == null || categories.isEmpty()) return;
        
        // Reset positions to their current order
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }
        
        // Update database
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (CategoryEntity category : categories) {
                    categoryDao.update(category);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadProductsForCategory(int categoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            java.util.List<ProductEntity> entities = productDao.getByCategory(categoryId);
            runOnUiThread(() -> {
                productList.clear();
                for (ProductEntity entity : entities) {
                    productList.add(Product.fromEntity(entity));
                }
                // Sort by position to maintain correct order
                productList.sort((p1, p2) -> Integer.compare(p1.getPosition(), p2.getPosition()));
                adapter.notifyDataSetChanged();
            });
        });
    }
}
