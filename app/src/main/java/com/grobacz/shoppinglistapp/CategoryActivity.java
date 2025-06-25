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
                                Log.d("CategoryAdapter", "After deletion, got " + updatedList.size() + " categories");
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
        listView.setAdapter(adapter);
        
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
        
        // Set up drag listener for the list view
        listView.setOnDragListener((v, event) -> {
            View view = (View) event.getLocalState();
            if (view == null) return false;
            
            // Get the background color for highlighting
            int highlightColor = getResources().getColor(R.color.drag_target_background, getTheme());
            
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d("CategoryActivity", "Drag started, view: " + view);
                    // Store the original background color
                    if (view.getTag(R.id.original_background) == null) {
                        view.setTag(R.id.original_background, view.getBackground());
                    }
                    // Apply elevation to the dragged view for better visual feedback
                    view.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100);
                    return true;
                    
                case DragEvent.ACTION_DRAG_LOCATION:
                    // Get the current position under the drag location
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int position = listView.pointToPosition(x, y);
                    
                    // Highlight the position where the item would be dropped
                    if (position != ListView.INVALID_POSITION) {
                        // Reset all views first
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            View child = listView.getChildAt(i);
                            Object originalBg = child.getTag(R.id.original_background);
                            if (originalBg != null) {
                                child.setBackground((Drawable) originalBg);
                            } else {
                                child.setBackgroundColor(Color.TRANSPARENT);
                            }
                        }
                        
                        // Highlight the target position
                        int firstVisible = listView.getFirstVisiblePosition();
                        int childIndex = position - firstVisible;
                        if (childIndex >= 0 && childIndex < listView.getChildCount()) {
                            View targetView = listView.getChildAt(childIndex);
                            if (targetView != null && position != draggedPosition) {
                                targetView.setBackgroundColor(highlightColor);
                            }
                        }
                    }
                    return true;
                    
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d("CategoryActivity", "Drag ended 1, draggedView: " + (draggedView != null));
                    runOnUiThread(() -> {
                        // Reset all views
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            View child = listView.getChildAt(i);
                            Object originalBg = child.getTag(R.id.original_background);
                            if (originalBg != null) {
                                child.setBackground((Drawable) originalBg);
                            } else {
                                child.setBackgroundColor(Color.TRANSPARENT);
                            }
                        }
                        
                        if (draggedView != null) {
                            // Animate the view back to its original state
                            draggedView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    if (draggedView != null) {
                                        draggedView.setVisibility(View.VISIBLE);
                                        draggedView = null;
                                    }
                                });
                        }
                    });
                    return true;
                    
                case DragEvent.ACTION_DROP:
                    Log.d("CategoryActivity", "Drop event, draggedPosition: " + draggedPosition);
                    
                    // Get the position where the drop occurred
                    int xPos = (int) event.getX();
                    int yPos = (int) event.getY();
                    int targetPosition = listView.pointToPosition(xPos, yPos);
                    
                    Log.d("CategoryActivity", "Dropped at position: " + targetPosition + ", dragged from: " + draggedPosition);
                    
                    if (draggedView == null || draggedPosition == -1) {
                        Log.w("CategoryActivity", "Invalid drop: no dragged view or position");
                        return false;
                    }
                    
                    if (targetPosition == ListView.INVALID_POSITION) {
                        Log.w("CategoryActivity", "Invalid drop: invalid target position");
                        return false;
                    }
                    
                    if (targetPosition >= categoryList.size()) {
                        targetPosition = categoryList.size() - 1;
                    }
                    
                    if (draggedPosition != targetPosition) {
                        handleItemMove(draggedPosition, targetPosition);
                    } else {
                        Log.d("CategoryActivity", "No position change, ignoring drop");
                    }
                    
                    // Reset the dragged view's visibility and background
                    runOnUiThread(() -> {
                        if (draggedView != null) {
                            Object originalBg = draggedView.getTag(R.id.original_background);
                            if (originalBg != null) {
                                draggedView.setBackground((Drawable) originalBg);
                            }
                            draggedView.setVisibility(View.VISIBLE);
                            draggedView = null;
                        }
                    });
                    
                    return true;
                    
                default:
                    return false;
            }
        });
        
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
                        view.setVisibility(View.INVISIBLE);
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
                    v.setVisibility(View.INVISIBLE);
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
        
        listView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                View view = (View) event.getLocalState();
                Log.d("CategoryActivity", "Drag event: " + eventToString(event.getAction()) + ", view: " + view);
                
                // Get the background color for highlighting
                int highlightColor = getResources().getColor(R.color.drag_target_background, getTheme());
                
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        Log.d("CategoryActivity", "Drag started, view: " + view);
                        if (view == null) {
                            return false;
                        }
                        // Apply elevation to the dragged view for better visual feedback
                        view.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100);
                        return true;
                        
                    case DragEvent.ACTION_DRAG_ENTERED:
                        Log.d("CategoryActivity", "Drag entered: " + v.getTag(R.id.tag_position));
                        if (view != v) {  // Don't highlight the view being dragged
                            v.setBackgroundColor(highlightColor);
                            // Add a subtle elevation to the target view
                            v.setElevation(8f);
                            v.invalidate();
                        }
                        return true;
                        
                    case DragEvent.ACTION_DRAG_LOCATION:
                        // Get the current position under the drag location
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        int position = listView.pointToPosition(x, y);
                        Log.d("CategoryActivity", "Drag location - x: " + x + ", y: " + y + ", position: " + position);
                        return true;
                        
                    case DragEvent.ACTION_DRAG_EXITED:
                        Log.d("CategoryActivity", "Drag exited: " + v.getTag(R.id.tag_position));
                        v.setBackgroundColor(Color.TRANSPARENT);
                        v.setElevation(0f);
                        v.invalidate();
                        return true;
                        
                    case DragEvent.ACTION_DRAG_ENDED:
                        Log.d("CategoryActivity", "Drag ended 2, draggedView: " + (draggedView != null));
                        runOnUiThread(() -> {
                            if (draggedView != null) {
                                // Animate the view back to its original state
                                draggedView.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .withEndAction(() -> {
                                        if (draggedView != null) {
                                            draggedView.setVisibility(View.VISIBLE);
                                            draggedView = null;
                                        }
                                    });
                            }
                            // Reset all views in the list
                            for (int i = 0; i < listView.getChildCount(); i++) {
                                View child = listView.getChildAt(i);
                                child.setBackgroundColor(Color.TRANSPARENT);
                                child.setElevation(0f);
                            }
                        });
                        draggedPosition = -1;
                        return true;
                        
                    case DragEvent.ACTION_DROP:
                        Log.d("CategoryActivity", "Drop event, draggedPosition: " + draggedPosition);
                        
                        // Get the position where the drop occurred
                        int xPos = (int) event.getX();
                        int yPos = (int) event.getY();
                        int targetPosition = listView.pointToPosition(xPos, yPos);
                        
                        Log.d("CategoryActivity", "Dropped at position: " + targetPosition + ", dragged from: " + draggedPosition);
                        
                        if (draggedView == null || draggedPosition == -1) {
                            Log.w("CategoryActivity", "Invalid drop: no dragged view or position");
                            return false;
                        }
                        
                        if (targetPosition == ListView.INVALID_POSITION) {
                            Log.w("CategoryActivity", "Invalid drop: invalid target position");
                            return false;
                        }
                        
                        if (targetPosition >= categoryList.size()) {
                            targetPosition = categoryList.size() - 1;
                        }
                        
                        if (draggedPosition != targetPosition) {
                            try {
                                // Create a new list to avoid concurrent modification
                                List<CategoryEntity> newList = new ArrayList<>(categoryList);
                                
                                // Get the item being moved
                                CategoryEntity draggedItem = newList.get(draggedPosition);
                                Log.d("CategoryActivity", "Moving category: " + draggedItem.getName() + 
                                      " from " + draggedPosition + " to " + targetPosition);
                                
                                // Remove from old position and add to new position
                                newList.remove(draggedPosition);
                                // Adjust target position if we removed an item before the target
                                if (targetPosition > draggedPosition) {
                                    targetPosition--;
                                }
                                newList.add(targetPosition, draggedItem);
                                
                                // Update the local list with the new order
                                Log.d("CategoryActivity", "Updating categoryList with new order");
                                synchronized (categoryList) {
                                    categoryList.clear();
                                    categoryList.addAll(newList);
                                }
                                Log.d("CategoryActivity", "categoryList has " + categoryList.size() + " items");
                                
                                // Update positions in the database on a background thread
                                new Thread(() -> {
                                    try {
                                        for (int i = 0; i < categoryList.size(); i++) {
                                            CategoryEntity item = categoryList.get(i);
                                            if (item.getPosition() != i) {
                                                Log.d("CategoryActivity", "Updating position for " + item.getName() + 
                                                      " to " + i);
                                                item.setPosition(i);
                                                categoryDao.updatePosition(item.getId(), i);
                                            }
                                        }
                                        Log.d("CategoryActivity", "Positions updated in database");
                                    } catch (Exception e) {
                                        Log.e("CategoryActivity", "Error updating positions: " + e.getMessage(), e);
                                        runOnUiThread(() -> 
                                            Toast.makeText(CategoryActivity.this, "Error updating positions", Toast.LENGTH_SHORT).show());
                                    }
                                }).start();
                                
                                // Update the UI on the main thread
                                runOnUiThread(() -> {
                                    try {
                                        // Create a new list for the adapter to avoid clearing the original list
                                        List<CategoryEntity> adapterList = new ArrayList<>(categoryList);
                                        adapter.clear();
                                        adapter.addAll(adapterList);
                                        adapter.notifyDataSetChanged();
                                        Log.d("CategoryActivity", "Adapter updated with new positions, item count: " + adapter.getCount());
                                    } catch (Exception e) {
                                        Log.e("CategoryActivity", "Error updating adapter: " + e.getMessage(), e);
                                    }
                                });
                                
                            } catch (Exception e) {
                                Log.e("CategoryActivity", "Error during drop: " + e.getMessage(), e);
                                runOnUiThread(() -> 
                                    Toast.makeText(CategoryActivity.this, "Error moving category", Toast.LENGTH_SHORT).show());
                                return false;
                            }
                        } else {
                            Log.d("CategoryActivity", "No position change, ignoring drop");
                        }
                        
                        // Reset the dragged view's visibility
                        runOnUiThread(() -> {
                            if (draggedView != null) {
                                draggedView.setVisibility(View.VISIBLE);
                                draggedView = null;
                            }
                            // Reset all backgrounds
                            for (int i = 0; i < listView.getChildCount(); i++) {
                                View child = listView.getChildAt(i);
                                child.setBackgroundColor(Color.TRANSPARENT);
                                child.setElevation(0f);
                            }
                        });
                        
                        return true;
                        
                    default:
                        Log.d("CategoryActivity", "Unhandled drag event: " + event.getAction());
                        return false;
                }
            }
        });
    }

    private void hideInputBar(LinearLayout inputBar, EditText editText) {
        inputBar.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> {
                inputBar.setVisibility(View.GONE);
                // Hide keyboard
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
                    // Hide the input bar with animation
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
                view.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                Log.e("CategoryActivity", "Error starting drag: " + e.getMessage(), e);
            }
        } else {
            Log.e("CategoryActivity", "Invalid position tag: " + tag);
        }
    }
    
    private ListView listView; // Add this line at the top with other fields
    
    private void handleItemMove(int fromPosition, int toPosition) {
        Log.d("CategoryActivity", "handleItemMove from " + fromPosition + " to " + toPosition);
        if (fromPosition < 0 || fromPosition >= categoryList.size() || toPosition < 0 || toPosition >= categoryList.size()) {
            Log.e("CategoryActivity", "Invalid positions: from=" + fromPosition + ", to=" + toPosition);
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
                
                // Get the updated list from the database to ensure consistency
                final List<CategoryEntity> freshList = categoryDao.getAll();
                
                // Update UI on the main thread
                runOnUiThread(() -> {
                    try {
                        if (adapter != null) {
                            // Update the main list with fresh data from the database
                            categoryList.clear();
                            categoryList.addAll(freshList);
                            
                            // Update the adapter
                            adapter.clear();
                            adapter.addAll(freshList);
                            adapter.notifyDataSetChanged();
                            
                            Log.d("CategoryActivity", "Adapter updated with fresh list, size: " + freshList.size());
                            
                            // Force a layout refresh
                            listView.invalidateViews();
                            
                            // Ensure the list is visible
                            listView.setVisibility(View.VISIBLE);
                            if (listView.getEmptyView() != null) {
                                listView.getEmptyView().setVisibility(View.GONE);
                            }
                        } else {
                            Log.e("CategoryActivity", "Adapter is null in handleItemMove");
                        }
                    } catch (Exception e) {
                        Log.e("CategoryActivity", "Error updating adapter: " + e.getMessage(), e);
                        // If there's an error, try to refresh the list
                        refreshList(listView);
                    }
                });
            } catch (Exception e) {
                Log.e("CategoryActivity", "Error updating category positions: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(CategoryActivity.this, "Error updating positions: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    // If there's an error, try to refresh the list
                    refreshList(listView);
                });
            }
        }).start();
    }
    
    // Helper method to dump view hierarchy for debugging
    private void dumpViewHierarchy(ViewGroup root, int depth) {
        if (root == null) return;
        
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        
        Log.d("ViewHierarchy", indent.toString() + root.getClass().getSimpleName() + 
            " [" + root.getWidth() + "x" + root.getHeight() + 
            "] visibility: " + (root.getVisibility() == View.VISIBLE ? "VISIBLE" : 
                               (root.getVisibility() == View.INVISIBLE ? "INVISIBLE" : "GONE")));
        
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                dumpViewHierarchy((ViewGroup) child, depth + 1);
            } else {
                Log.d("ViewHierarchy", indent.toString() + "  " + child.getClass().getSimpleName() + 
                    " [" + child.getWidth() + "x" + child.getHeight() + 
                    "] visibility: " + (child.getVisibility() == View.VISIBLE ? "VISIBLE" : 
                                       (child.getVisibility() == View.INVISIBLE ? "INVISIBLE" : "GONE")));
            }
        }
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
