package com.grobacz.shoppinglistapp;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

class CategoryAdapter extends ArrayAdapter<CategoryEntity> {
    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final Context context;

    public CategoryAdapter(Context context, List<CategoryEntity> categories, 
                         CategoryDao categoryDao, ProductDao productDao) {
        super(context, 0, categories);
        this.context = context;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.category_list_item, parent, false);
        }

        CategoryEntity category = getItem(position);
        if (category != null) {
            TextView categoryName = convertView.findViewById(R.id.category_name);
            ImageButton deleteButton = convertView.findViewById(R.id.delete_button);
            
            categoryName.setText(category.name);
            
            deleteButton.setOnClickListener(v -> {
                new CheckProductsAndDeleteTask(context, category, categoryDao, productDao, 
                    () -> {
                        // Refresh the list after deletion
                        clear();
                        addAll(categoryDao.getAll());
                        notifyDataSetChanged();
                    }).execute();
            });
        }
        
        return convertView;
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
        List<ProductEntity> products = productDao.getByCategory(category.id);
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
    private CategoryAdapter adapter;
    private List<CategoryEntity> categoryList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = AppDatabaseSingleton.getInstance(this);
        categoryDao = db.categoryDao();

        ListView listView = findViewById(R.id.category_list_view);
        EditText editText = findViewById(R.id.category_edit_text);
        Button addButton = findViewById(R.id.add_category_button);
        Button backButton = findViewById(R.id.back_to_products_button);
        Button saveButton = findViewById(R.id.save_category_button);
        LinearLayout inputBar = findViewById(R.id.input_bar);

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

    private void refreshList(ListView listView) {
        new Thread(() -> {
            categoryList = categoryDao.getAll();
            runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new CategoryAdapter(this, categoryList, categoryDao, db.productDao());
                    listView.setAdapter(adapter);
                } else {
                    adapter.clear();
                    adapter.addAll(categoryList);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }
}
