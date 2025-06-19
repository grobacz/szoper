package com.grobacz.shoppinglistapp;

import java.util.List; // Needed for List usage

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import com.grobacz.shoppinglistapp.Product;

import androidx.room.Room;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        // Check database state
        AppDatabase.checkDatabase(this);
        // Force reload categories and tabs
        runOnUiThread(this::loadCategoriesAndTabs);
    }
    
    // Method to reset the database for testing
    public static void resetDatabase(android.content.Context context) {
        android.util.Log.d("AppDatabase", "Resetting database...");
        context.deleteDatabase("shopping_list_database");
        // Reset the singleton instance
        AppDatabaseSingleton.resetInstance();
        android.util.Log.d("AppDatabase", "Database reset complete");
    }

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ArrayList<Product> productList;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabBurger;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private AppDatabase db;
    private ProductDao productDao;
    private CategoryDao categoryDao;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private java.util.List<CategoryEntity> categories;
    private int selectedCategoryId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tabLayout = findViewById(R.id.tabLayout);

        // Request BLUETOOTH_CONNECT permission at runtime for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1001);
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        db = AppDatabaseSingleton.getInstance(getApplicationContext());
        productDao = db.productDao();
        categoryDao = db.categoryDao();
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList);

        loadCategoriesAndTabs();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Attach ItemTouchHelper for swipe gesture
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.RIGHT | androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Product product = productList.get(position);
                if (direction == androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                    if (product.getQuantity() > 0) {
                        // Decrement quantity
                        Product updated = new Product(product.getName(), product.getQuantity() - 1, product.getCategoryId());
                        updated.updateLastModified();
                        productList.set(position, updated);
                        adapter.notifyItemChanged(position);
                        // Update in DB
                        Executors.newSingleThreadExecutor().execute(() -> {
                            // Always update the product in DB, even if quantity is 0
                            productDao.insert(updated.toEntity());
                        });
                    } else {
                        // Remove if already 0
                        Product removed = productList.remove(position);
                        adapter.notifyItemRemoved(position);
                        // Remove from DB
                        Executors.newSingleThreadExecutor().execute(() -> {
                            productDao.deleteByName(removed.getName());
                        });
                    }
                } else if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                    // Increment quantity
                    Product updated = new Product(product.getName(), product.getQuantity() + 1, product.getCategoryId());
                    updated.updateLastModified();
                    productList.set(position, updated);
                    adapter.notifyItemChanged(position);
                    // Update in DB
                    Executors.newSingleThreadExecutor().execute(() -> {
                        productDao.insert(updated.toEntity());
                    });
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddProductDialog());

        fabBurger = findViewById(R.id.fabBurger);
        fabBurger.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, fabBurger);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
            popup.show();
        });

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (categories != null && pos < categories.size()) {
                    selectedCategoryId = categories.get(pos).id;
                    loadProductsForCategory(selectedCategoryId);
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        // Start Bluetooth server thread
        new Thread(this::startServer).start();
    }

    private void showAddProductDialog() {
        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);

        EditText editTextName = view.findViewById(R.id.etProductName);
        EditText editTextQuantity = view.findViewById(R.id.etQuantity);
        ImageButton btnAdd = view.findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String quantityStr = editTextQuantity.getText().toString().trim();
            int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);
            if (!name.isEmpty()) {
                // Create product with current timestamp
                Product product = new Product(name, quantity, selectedCategoryId);
                product.updateLastModified();
                productList.add(product);
                adapter.notifyItemInserted(productList.size() - 1);
                // Save to database
                Executors.newSingleThreadExecutor().execute(() -> {
                    ProductEntity entity = product.toEntity();
                    productDao.insert(entity);
                });
            }
            dialog.dismiss();
        }); 
        dialog.setContentView(view);
        dialog.show();
    }

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

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final ArrayList<Product> products;

        ProductAdapter(ArrayList<Product> products) {
            this.products = products;
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
            holder.txtProductName.setText(product.getName());
            holder.txtQuantity.setText(String.valueOf(product.getQuantity()));
            // Strikethrough if quantity is 0
            if (product.getQuantity() == 0) {
                holder.txtProductName.setPaintFlags(holder.txtProductName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.txtQuantity.setPaintFlags(holder.txtQuantity.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.txtProductName.setPaintFlags(holder.txtProductName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                holder.txtQuantity.setPaintFlags(holder.txtQuantity.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView txtProductName;
            TextView txtQuantity;
            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                txtProductName = itemView.findViewById(R.id.txtProductName);
                txtQuantity = itemView.findViewById(R.id.txtQuantity);
            }
        }
    }

    // BluetoothHandler inner class, now properly nested inside MainActivity
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
                List<CategoryEntity> categoryList = categoryDao.getAll();
                List<ProductEntity> productEntities = productDao.getAll();
                List<Product> productList = new ArrayList<>();
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
                        if (categoryDao.getByName(cat.name) == null) {
                            categoryDao.insert(cat);
                        }
                    }
                    // 5. Synchronize products (with category relationship)
                    for (Product prod : receivedData.products) {
                        if (productDao.getByName(prod.getName()) == null) {
                            // Map category name to local categoryId if needed
                            CategoryEntity localCat = categoryDao.getById(prod.getCategoryId());
                            if (localCat != null) {
                                prod.setCategoryId(localCat.id);
                            }
                            // Convert Product to ProductEntity before inserting
                            ProductEntity prodEntity = prod.toEntity();
                            productDao.insert(prodEntity); // productDao.insert expects ProductEntity
                        }
                    }
                    // 6. Update UI as needed
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                });
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_sync) {
            showSyncDialog();
            return true;
        } else if (id == R.id.action_categories) {
            Intent intent = new Intent(this, CategoryActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_reset_db) {
            // Reset the database
            AppDatabase.resetDatabase(this);
            // Restart the activity to reinitialize everything
            finish();
            startActivity(getIntent());
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void loadCategoriesAndTabs() {       
        Executors.newSingleThreadExecutor().execute(() -> {
            try {                
                // Get all categories
                List<CategoryEntity> currentCategories = categoryDao.getAll();
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    if (tabLayout != null) {
                        tabLayout.removeAllTabs();
                        for (CategoryEntity cat : currentCategories) {
                            tabLayout.addTab(tabLayout.newTab().setText(cat.name));
                        }
                        
                        if (!currentCategories.isEmpty()) {
                            selectedCategoryId = currentCategories.get(0).id;
                            loadProductsForCategory(selectedCategoryId);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading categories: " + e.getMessage(), e);
            }
        });
    }

    private void loadProductsForCategory(int categoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            java.util.List<ProductEntity> entities = productDao.getByCategory(categoryId);
            productList.clear();
            for (ProductEntity entity : entities) {
                productList.add(Product.fromEntity(entity));
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }
}
