package com.grobacz.shoppinglistapp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List; // Needed for List usage
import android.view.Menu;
import android.view.MenuItem;
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
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.grobacz.shoppinglistapp.CategoryTabLayout;

import com.grobacz.shoppinglistapp.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import androidx.room.Room;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    // Interface moved to separate file

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
    public void onTabSelected(TabLayout.Tab tab) {
        Object tag = tab.getTag();
        if (tag != null) {
            selectedCategoryId = (int) tag;
            loadProductsForCategory(selectedCategoryId);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        db = AppDatabaseSingleton.getInstance(getApplicationContext());
        productDao = db.productDao();
        categoryDao = db.categoryDao();

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

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList, this::updateProductPositions);
        recyclerView.setAdapter(adapter);
        
        // Add divider between items
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        // Set up FAB click listener
        fabAdd.setOnClickListener(v -> showAddProductDialog());

        // Load categories and set up tab selection
        // Set up tab layout
        tabLayout.addOnTabSelectedListener(this);

        fabBurger = findViewById(R.id.fabBurger);
        fabBurger.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, fabBurger);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
            popup.show();
        });

        // Start Bluetooth server thread
        new Thread(this::startServer).start();
    }

    private void showAddProductDialog() {
        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Please select a category first", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_product, null);
        dialog.setContentView(dialogView);

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        ImageButton btnAdd = dialogView.findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);

            if (!productName.isEmpty()) {
                // Get next position for the product in this category
                int nextPosition = 0;
                if (!productList.isEmpty()) {
                    nextPosition = productList.get(productList.size() - 1).getPosition() + 1;
                }

                // Create and add the product
                Product product = new Product(
                    productName,
                    quantity,
                    selectedCategoryId,
                    System.currentTimeMillis(),
                    nextPosition
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

        dialog.show();
    }

    private void updateProductPositions() {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (int i = 0; i < adapter.products.size(); i++) {
                Product product = adapter.products.get(i);
                ProductEntity entity = productDao.getByName(product.getName());
                if (entity != null) {
                    entity.setPosition(i);
                    productDao.update(entity);
                }
            }
        });
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
                    
                    if (fromPosition < toPosition) {
                        for (int i = fromPosition; i < toPosition; i++) {
                            Collections.swap(products, i, i + 1);
                        }
                    } else {
                        for (int i = fromPosition; i > toPosition; i--) {
                            Collections.swap(products, i, i - 1);
                        }
                    }
                    
                    // Update positions in the database by running the callback on the UI thread
                    if (updatePositionsCallback != null) {
                        activity.runOnUiThread(updatePositionsCallback);
                    }

                    notifyItemMoved(fromPosition, toPosition);
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
                        // Swipe right - decrease quantity, cross out, or remove
                        if (originalProduct.getQuantity() > 1) {
                            // Decrease quantity
                            final int newQuantity = originalProduct.getQuantity() - 1;
                            
                            final Product updated = new Product(
                                originalProduct.getName(),
                                newQuantity,
                                originalProduct.getCategoryId(),
                                System.currentTimeMillis(),
                                originalProduct.getPosition()
                            );
                            
                            activity.runOnUiThread(() -> {
                                if (position < products.size()) {
                                    products.set(position, updated);
                                    notifyItemChanged(position);
                                    
                                    // Update in database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        activity.productDao.insert(updated.toEntity());
                                    });
                                }
                            });
                        } else if (originalProduct.getQuantity() == 1) {
                            // Set quantity to 0 and cross out
                            final Product updated = new Product(
                                originalProduct.getName(),
                                0,
                                originalProduct.getCategoryId(),
                                System.currentTimeMillis(),
                                originalProduct.getPosition()
                            );
                            
                            activity.runOnUiThread(() -> {
                                if (position < products.size()) {
                                    products.set(position, updated);
                                    notifyItemChanged(position);
                                    
                                    // Update in database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        activity.productDao.insert(updated.toEntity());
                                    });
                                }
                            });
                        } else {
                            // Remove if quantity is already 0
                            activity.runOnUiThread(() -> {
                                if (position < products.size() && products.get(position).equals(originalProduct)) {
                                    Product removed = products.remove(position);
                                    notifyItemRemoved(position);
                                    
                                    // Delete from database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        activity.productDao.delete(removed.toEntity());
                                    });
                                }
                            });
                        }
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left - increase quantity
                        final int newQuantity = originalProduct.getQuantity() + 1;
                        
                        final Product updated = new Product(
                            originalProduct.getName(),
                            newQuantity,
                            originalProduct.getCategoryId(),
                            System.currentTimeMillis(),
                            originalProduct.getPosition()
                        );
                        
                        activity.runOnUiThread(() -> {
                            if (position < products.size()) {
                                products.set(position, updated);
                                notifyItemChanged(position);
                                
                                // Update in database
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    activity.productDao.insert(updated.toEntity());
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
            // Update the items in the list
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(products, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(products, i, i - 1);
                }
            }
            
            // Notify the adapter that the item has moved
            notifyItemMoved(fromPosition, toPosition);
            
            // Update positions in the database by running the callback on the UI thread
            if (updatePositionsCallback != null) {
                activity.runOnUiThread(updatePositionsCallback);
            }
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtProductName;
            private final TextView txtQuantity;
            private final ImageView dragHandle;

            ProductViewHolder(View itemView) {
                super(itemView);
                txtProductName = itemView.findViewById(R.id.txtProductName);
                txtQuantity = itemView.findViewById(R.id.txtQuantity);
                dragHandle = itemView.findViewById(R.id.drag_handle);
            }

            void bind(Product product) {
                txtProductName.setText(product.getName());
                txtQuantity.setText(String.valueOf(product.getQuantity()));

                // Strikethrough if quantity is 0
                if (product.getQuantity() == 0) {
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
                List<CategoryEntity> categoryList = categoryDao.getAll();
                List<ProductEntity> productEntities = productDao.getAllProducts();
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
                        if (categoryDao.getCategoryByName(cat.getName()) == null) {
                            categoryDao.insert(cat);
                        }
                    }
                    // 5. Synchronize products (with category relationship)
                    for (Product prod : receivedData.products) {
                        if (productDao.getByName(prod.getName()) == null) {
                            // Map category name to local categoryId if needed
                            CategoryEntity localCat = categoryDao.getById(prod.getCategoryId());
                            if (localCat != null) {
                                prod.setCategoryId(localCat.getId());
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
        Executors.newSingleThreadExecutor().execute(() -> {
            try {                
                // Get all categories, ordered by position
                categories = categoryDao.getAll();
                
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
            productList.clear();
            for (ProductEntity entity : entities) {
                productList.add(Product.fromEntity(entity));
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }
}
