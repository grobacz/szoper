package com.grobacz.shoppinglistapp;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

class CategoryAdapter extends ArrayAdapter<CategoryEntity> {
    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final Context context;
    private Activity activity;
    private View.OnLongClickListener onItemLongClickListener;
    private View.OnTouchListener onDragHandleTouchListener;

    public CategoryAdapter(@NonNull Context context, List<CategoryEntity> categories, 
                         CategoryDao categoryDao, ProductDao productDao) {
        super(context, 0, categories);
        this.context = context;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        if (context instanceof Activity) {
            this.activity = (Activity) context;
            Log.d("CategoryAdapter", "Adapter created with " + categories.size() + " categories");
        } else {
            throw new IllegalArgumentException("Context must be an Activity");
        }
    }
    
    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
    
    public void setOnDragHandleTouchListener(View.OnTouchListener listener) {
        this.onDragHandleTouchListener = listener;
    }

    // ViewHolder pattern for better performance
    private static class ViewHolder {
        TextView categoryName;
        ImageButton deleteButton;
        ImageView dragHandle;
        int position;
    }
    
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Ensure the view is visible
        if (convertView != null) {
            convertView.setVisibility(View.VISIBLE);
        }
        
        Log.d("CategoryAdapter", "getView called for position: " + position);
        View itemView = convertView;
        ViewHolder holder;
        
        if (itemView == null) {
            Log.d("CategoryAdapter", "Creating new view for position: " + position);
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.category_list_item, parent, false);
            holder = new ViewHolder();
            holder.categoryName = itemView.findViewById(R.id.category_name);
            holder.deleteButton = itemView.findViewById(R.id.delete_button);
            holder.dragHandle = itemView.findViewById(R.id.drag_handle);
            
            if (holder.categoryName == null) Log.e("CategoryAdapter", "categoryName view not found!");
            if (holder.deleteButton == null) Log.e("CategoryAdapter", "deleteButton view not found!");
            if (holder.dragHandle == null) Log.e("CategoryAdapter", "dragHandle view not found!");
            
            itemView.setTag(holder);
            
            // Set up drag handle touch listener
            if (onDragHandleTouchListener != null) {
                holder.dragHandle.setOnTouchListener(onDragHandleTouchListener);
            } else {
                Log.e("CategoryAdapter", "onDragHandleTouchListener is null!");
            }
            
            // Set up item long click listener
            if (onItemLongClickListener != null) {
                itemView.setOnLongClickListener(onItemLongClickListener);
            } else {
                Log.e("CategoryAdapter", "onItemLongClickListener is null!");
            }
            
            // Set up delete button click listener
            holder.deleteButton.setOnClickListener(v -> {
                int pos = (int) v.getTag(R.id.tag_position);
                Log.d("CategoryAdapter", "Delete button clicked for position: " + pos);
                CategoryEntity cat = getItem(pos);
                if (cat != null) {
                    Log.d("CategoryAdapter", "Deleting category: " + cat.getName());
                    new CheckProductsAndDeleteTask(context, cat, categoryDao, productDao, () -> 
                        activity.runOnUiThread(() -> {
                            try {
                                List<CategoryEntity> updatedList = categoryDao.getAll();
                                Log.d("CategoryActivity", "After deletion, got " + updatedList.size() + " categories");
                                clear();
                                addAll(updatedList);
                                notifyDataSetChanged();
                            } catch (Exception e) {
                                Log.e("CategoryAdapter", "Error updating after delete", e);
                            }
                        })
                    ).execute();
                } else {
                    Log.e("CategoryAdapter", "Category is null at position: " + pos);
                }
            });
        } else {
            holder = (ViewHolder) itemView.getTag();
        }
        
        // Get the category for this position
        CategoryEntity category = getItem(position);
        if (category != null) {
            Log.d("CategoryAdapter", "Setting up view for category: " + category.getName() + " at position " + position);
            // Store the position in the view's tag
            holder.position = position;
            itemView.setTag(R.id.tag_position, position);
            holder.dragHandle.setTag(R.id.tag_position, position);
            holder.deleteButton.setTag(R.id.tag_position, position);
            
            // Set the category name
            holder.categoryName.setText(category.getName());
            Log.d("CategoryAdapter", "Set text to: " + category.getName());
        } else {
            Log.e("CategoryAdapter", "Category is null at position: " + position);
            holder.categoryName.setText("<Error loading category>");
        }
        
        // Make sure the view is visible
        itemView.setVisibility(View.VISIBLE);
        Log.d("CategoryAdapter", "View setup complete for position: " + position);
        
        return itemView;
    }
}

class CheckProductsAndDeleteTask extends AsyncTask<Void, Void, Boolean> {
    private final Context context;
    private final CategoryEntity category;
    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final Runnable onSuccess;
    private int productCount;

    public CheckProductsAndDeleteTask(Context context, CategoryEntity category, 
                                    CategoryDao categoryDao, ProductDao productDao,
                                    Runnable onSuccess) {
        this.context = context;
        this.category = category;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        this.onSuccess = onSuccess;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        List<ProductEntity> products = productDao.getByCategory(category.getId());
        productCount = products.size();
        return productCount > 0;
    }

    @Override
    protected void onPostExecute(Boolean hasProducts) {
        if (hasProducts) {
            // Show confirmation dialog for categories with products
            new AlertDialog.Builder(context)
                .setTitle(R.string.delete_category)
                .setMessage(R.string.category_has_products)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCategory())
                .setNegativeButton(R.string.cancel, null)
                .show();
        } else {
            // No products, delete directly
            deleteCategory();
        }
    }

    private void deleteCategory() {
        new Thread(() -> {
            categoryDao.delete(category);
            ((android.app.Activity) context).runOnUiThread(onSuccess);
        }).start();
    }
}

public class CategoryActivity extends AppCompatActivity {
    private AppDatabase db;
    private CategoryDao categoryDao;
    private ProductDao productDao;
    private CategoryAdapter adapter;
    private List<CategoryEntity> categoryList;
    private View draggedView = null;
    private int draggedPosition = -1;
    private View.OnTouchListener dragHandleTouchListener;
    private View.OnLongClickListener itemLongClickListener;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = AppDatabaseSingleton.getInstance(this);
        categoryDao = db.categoryDao();
        productDao = db.productDao();

        // Initialize the list first
        categoryList = new ArrayList<>();
        
        // Initialize the listView field
        listView = findViewById(R.id.category_list_view);
        // Ensure the list view is visible and has proper layout params
        listView.setVisibility(View.VISIBLE);
        listView.setEmptyView(findViewById(android.R.id.empty));
        
        // Add an empty view in case there are no categories
        TextView emptyView = new TextView(this);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText("No categories yet. Add one to get started!");
        emptyView.setVisibility(View.GONE);
        ((ViewGroup)listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);
        EditText editText = findViewById(R.id.category_edit_text);
        Button addButton = findViewById(R.id.add_category_button);
        Button backButton = findViewById(R.id.back_to_products_button);
        Button saveButton = findViewById(R.id.save_category_button);
        LinearLayout inputBar = findViewById(R.id.input_bar);

        // Set up the adapter with the empty list first
        adapter = new CategoryAdapter(this, categoryList, categoryDao, productDao);
        
        // Set up drag handle touch listener
        dragHandleTouchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startDrag((View) v.getParent());
                return true;
            }
            return false;
        };
        
        itemLongClickListener = v -> {
            startDrag(v);
            return true;
        };
        
        // Set the listeners to the adapter
        adapter.setOnDragHandleTouchListener(dragHandleTouchListener);
        adapter.setOnItemLongClickListener(itemLongClickListener);
        listView.setAdapter(adapter);
        
        // Initialize vibrator for haptic feedback
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Then load the data asynchronously
        refreshList(listView);

        addButton.setOnClickListener(v -> {
            if (inputBar.getVisibility() != View.VISIBLE) {
                // Show the input bar with animation
                inputBar.setVisibility(View.VISIBLE);
                inputBar.setAlpha(0f);
                inputBar.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
                editText.requestFocus();
                // Show soft keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            } else {
                // Hide the input bar if it's already visible
                hideInputBar(inputBar, editText);
            }
        });

        saveButton.setOnClickListener(v -> saveCategory(editText, listView, inputBar));

        // Handle done action on keyboard
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCategory(editText, listView, inputBar);
                return true;
            }
            return false;
        });

        // Remove the long click listener since we now have delete buttons

        // Hide input bar when clicking on the list
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (inputBar.getVisibility() == View.VISIBLE) {
                hideInputBar(inputBar, editText);
            }
        });

        backButton.setOnClickListener(v -> finish());

        // Initialize haptic feedback
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Set up drag and drop
        dragHandleTouchListener = (v, event) -> {
            Log.d("CategoryActivity", "Drag handle touched, action: " + event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Start drag when the drag handle is touched
                View view = (View) v.getParent();
                draggedView = view;
                
                // Get the position from the tag
                Object tag = view.getTag(R.id.tag_position);
                if (tag instanceof Integer) {
                    draggedPosition = (int) tag;
                    Log.d("CategoryActivity", "Starting drag from position: " + draggedPosition);
                    
                    // Provide haptic feedback
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            // Deprecated in API 26
                            vibrator.vibrate(50);
                        }
                    }
                    
                    // Start drag with a shadow
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            view.startDragAndDrop(null, shadowBuilder, view, 0);
                        } else {
                            view.startDrag(null, shadowBuilder, view, 0);
                        }
                        view.setAlpha(0.5f);
                        return true;
                    } catch (Exception e) {
                        Log.e("CategoryActivity", "Error starting drag: " + e.getMessage(), e);
                    }
                } else {
                    Log.e("CategoryActivity", "Invalid position tag: " + tag);
                }
            }
            return false;
        };
        
        itemLongClickListener = v -> {
            Log.d("CategoryActivity", "Item long clicked, starting drag");
            // Start drag on long press of the entire item
            draggedView = v;
            
            // Get the position from the tag
            Object tag = v.getTag(R.id.tag_position);
            if (tag instanceof Integer) {
                draggedPosition = (int) tag;
                Log.d("CategoryActivity", "Starting drag from position (long press): " + draggedPosition);
                
                // Provide haptic feedback
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        // Deprecated in API 26
                        vibrator.vibrate(50);
                    }
                }
                
                // Start drag with a shadow
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        v.startDragAndDrop(null, shadowBuilder, v, 0);
                    } else {
                        v.startDrag(null, shadowBuilder, v, 0);
                    }
                    v.setAlpha(0.5f);
                    return true;
                } catch (Exception e) {
                    Log.e("CategoryActivity", "Error starting drag (long press): " + e.getMessage(), e);
                }
            } else {
                Log.e("CategoryActivity", "Invalid position tag in long click: " + tag);
            }
            return false;
        };
        
        // Set up the adapter with the drag listeners
        adapter = new CategoryAdapter(this, categoryList, categoryDao, productDao);
        adapter.setOnDragHandleTouchListener(dragHandleTouchListener);
        adapter.setOnItemLongClickListener(itemLongClickListener);
        listView.setAdapter(adapter);
        
        // Set up drag and drop listener for the list view
        listView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                final int action = event.getAction();
                Log.d("CategoryActivity", "onDrag: " + eventToString(action));
                
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        // Return true to continue receiving drag events
                        return true;
                        
                    case DragEvent.ACTION_DRAG_ENTERED:
                        // No visual change needed when drag enters
                        return true;
                        
                    case DragEvent.ACTION_DRAG_EXITED:
                        // No visual change needed when drag exits
                        return true;
                        
                    case DragEvent.ACTION_DRAG_LOCATION: {
                        // Get the position of the item under the drag location
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int position = listView.pointToPosition(x, y);
                        
                        if (position != AdapterView.INVALID_POSITION && draggedPosition != position) {
                            // Handle the item move
                            handleItemMove(draggedPosition, position);
                            draggedPosition = position;
                        }
                        return true;
                    }
                        
                    case DragEvent.ACTION_DROP: {
                        // Get the position where the item was dropped
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int position = listView.pointToPosition(x, y);
                        
                        if (position != AdapterView.INVALID_POSITION) {
                            // Handle the final drop
                            handleItemMove(draggedPosition, position);
                        }
                        
                        // Reset the dragged view's alpha
                        if (draggedView != null) {
                            draggedView.setAlpha(1.0f);
                            draggedView = null;
                        }
                        return true;
                    }
                        
                    case DragEvent.ACTION_DRAG_ENDED:
                        // Reset the dragged view's alpha if the drag was cancelled
                        if (draggedView != null) {
                            draggedView.setAlpha(1.0f);
                            draggedView = null;
                        }
                        return true;
                }
                
                return false;
            }
        });
        
    }

    private ListView listView; // Add this line at the top with other fields

    private void hideInputBar(LinearLayout inputBar, EditText editText) {
        inputBar.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> {
                inputBar.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                editText.clearFocus();
            })
            .start();
    }

    private void saveCategory(EditText editText, ListView listView, LinearLayout inputBar) {
        String name = editText.getText().toString().trim();
        if (!name.isEmpty()) {
            new Thread(() -> {
                categoryDao.insert(new CategoryEntity(name));
                runOnUiThread(() -> {
                    editText.setText("");
                    refreshList(listView);
                    hideInputBar(inputBar, editText);
                });
            }).start();
        }
    }

    private void startDrag(View view) {
        draggedView = view;
        
        // Get the position from the tag
        Object tag = view.getTag(R.id.tag_position);
        if (tag instanceof Integer) {
            draggedPosition = (int) tag;
            Log.d("CategoryActivity", "Starting drag from position: " + draggedPosition);
            
            // Start drag with a shadow
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view.startDragAndDrop(null, shadowBuilder, view, 0);
                } else {
                    view.startDrag(null, shadowBuilder, view, 0);
                }
                view.setAlpha(0.5f);
            } catch (Exception e) {
                Log.e("CategoryActivity", "Error starting drag: " + e.getMessage(), e);
            }
        } else {
            Log.e("CategoryActivity", "Invalid position tag: " + tag);
        }
    }
    
    private void handleItemMove(int fromPosition, int toPosition) {
        Log.d("CategoryActivity", "handleItemMove from " + fromPosition + " to " + toPosition);
        if (fromPosition < 0 || fromPosition >= categoryList.size() || 
            toPosition < 0 || toPosition >= categoryList.size() || 
            fromPosition == toPosition) {
            Log.d("CategoryActivity", "No position change needed: from=" + fromPosition + ", to=" + toPosition);
            return;
        }
        
        // Make a copy of the current list to avoid concurrent modification
        final List<CategoryEntity> updatedList = new ArrayList<>(categoryList);
        
        // Get the item being moved
        CategoryEntity movedItem = updatedList.get(fromPosition);
        Log.d("CategoryActivity", "Moving category: " + movedItem.getName() + 
              " from " + fromPosition + " to " + toPosition);
        
        // Remove from old position and add to new position
        updatedList.remove(fromPosition);
        updatedList.add(toPosition, movedItem);
        
        // Update the main list
        categoryList.clear();
        categoryList.addAll(updatedList);
        
        // Immediately update the UI
        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.clear();
                adapter.addAll(updatedList);
                adapter.notifyDataSetChanged();
                
                // Ensure the moved item is visible
                listView.smoothScrollToPosition(toPosition);
            }
        });
        
        // Update positions in the database on a background thread
        new Thread(() -> {
            try {
                // Update positions in the database
                for (int i = 0; i < updatedList.size(); i++) {
                    CategoryEntity category = updatedList.get(i);
                    if (category.getPosition() != i) {
                        category.setPosition(i);
                        categoryDao.updatePosition(category.getId(), i);
                        Log.d("CategoryActivity", "Updated position for " + category.getName() + " to " + i);
                    }
                }
                Log.d("CategoryActivity", "All positions updated in database");
            } catch (Exception e) {
                Log.e("CategoryActivity", "Error updating category positions: " + e.getMessage(), e);
            }
        }).start();
    }
    
    private void checkDatabaseContents() {
        new Thread(() -> {
            try {
                List<CategoryEntity> allCategories = categoryDao.getAll();
                Log.d("DatabaseCheck", "Found " + allCategories.size() + " categories in database");
                for (CategoryEntity cat : allCategories) {
                    Log.d("DatabaseCheck", "Category: " + cat.getName() + " (id: " + cat.getId() + ", pos: " + cat.getPosition() + ")");
                }
            } catch (Exception e) {
                Log.e("DatabaseCheck", "Error reading database", e);
            }
        }).start();
    }
    
    private void refreshList(ListView listView) {
        if (listView == null) {
            Log.e("CategoryActivity", "refreshList: listView is null");
            return;
        }
        
        Log.d("CategoryActivity", "refreshList called, listView: " + listView);
        Log.d("CategoryActivity", "List view dimensions: " + listView.getWidth() + "x" + listView.getHeight());
        
        // Show loading indicator if needed
        runOnUiThread(() -> {
            listView.setVisibility(View.VISIBLE);
            if (listView.getEmptyView() != null) {
                listView.getEmptyView().setVisibility(View.GONE);
            }
        });
        
        new Thread(() -> {
            try {
                Log.d("CategoryActivity", "Loading categories from database");
                final List<CategoryEntity> newList = categoryDao.getAll();
                Log.d("CategoryActivity", "Loaded " + (newList != null ? newList.size() : 0) + " categories from database");
                
                if (newList != null) {
                    for (CategoryEntity cat : newList) {
                        Log.d("CategoryActivity", "Category: " + cat.getName() + " (id: " + cat.getId() + ", pos: " + cat.getPosition() + ")");
                    }
                }
                
                runOnUiThread(() -> {
                    try {
                        Log.d("CategoryActivity", "Updating UI with categories");
                        if (categoryList == null) {
                            Log.d("CategoryActivity", "Initializing new category list");
                            categoryList = new ArrayList<>();
                        } else {
                            Log.d("CategoryActivity", "Clearing existing category list");
                            categoryList.clear();
                        }
                        
                        if (newList != null && !newList.isEmpty()) {
                            Log.d("CategoryActivity", "Adding " + newList.size() + " categories to list");
                            categoryList.addAll(newList);
                            
                            if (adapter == null) {
                                Log.d("CategoryActivity", "Creating new adapter with " + newList.size() + " categories");
                                adapter = new CategoryAdapter(this, categoryList, categoryDao, productDao);
                                adapter.setOnDragHandleTouchListener(dragHandleTouchListener);
                                adapter.setOnItemLongClickListener(itemLongClickListener);
                                listView.setAdapter(adapter);
                                Log.d("CategoryActivity", "New adapter created and set on list view");
                            } else {
                                Log.d("CategoryActivity", "Updating adapter with " + newList.size() + " categories");
                                adapter.clear();
                                adapter.addAll(newList);
                                adapter.notifyDataSetChanged();
                                Log.d("CategoryActivity", "Adapter updated with new data");
                            }
                            
                            // Ensure the list is visible
                            listView.setVisibility(View.VISIBLE);
                            if (listView.getEmptyView() != null) {
                                listView.getEmptyView().setVisibility(View.GONE);
                            }
                        } else {
                            Log.d("CategoryActivity", "No categories to display");
                            // Show empty view if no categories
                            if (listView.getEmptyView() != null) {
                                listView.getEmptyView().setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CategoryActivity", "Error updating UI with category list", e);
                        Toast.makeText(CategoryActivity.this, "Error updating UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        
                        // On error, ensure we show the empty view
                        if (listView.getEmptyView() != null) {
                            listView.getEmptyView().setVisibility(View.VISIBLE);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("CategoryActivity", "Error loading categories from database", e);
                runOnUiThread(() -> {
                    Toast.makeText(CategoryActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // On error, ensure we show the empty view
                    if (listView.getEmptyView() != null) {
                        listView.getEmptyView().setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (draggedView != null) {
            draggedView.setVisibility(View.VISIBLE);
        }
    }
    
    private String eventToString(int action) {
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED: return "DRAG_STARTED";
            case DragEvent.ACTION_DRAG_ENTERED: return "DRAG_ENTERED";
            case DragEvent.ACTION_DRAG_LOCATION: return "DRAG_LOCATION";
            case DragEvent.ACTION_DRAG_EXITED: return "DRAG_EXITED";
            case DragEvent.ACTION_DROP: return "DROP";
            case DragEvent.ACTION_DRAG_ENDED: return "DRAG_ENDED";
            default: return "UNKNOWN(" + action + ")";
        }
    }
}
