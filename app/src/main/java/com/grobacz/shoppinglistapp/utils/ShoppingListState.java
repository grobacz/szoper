package com.grobacz.shoppinglistapp.utils;

import com.grobacz.shoppinglistapp.model.CategoryEntity;
import com.grobacz.shoppinglistapp.model.ProductEntity;

import java.util.List;

public class ShoppingListState {
    private List<CategoryEntity> categories;
    private List<ProductEntity> products;

    public ShoppingListState() {
        // Default constructor required for Gson
    }

    public ShoppingListState(List<CategoryEntity> categories, List<ProductEntity> products) {
        this.categories = categories;
        this.products = products;
    }

    public List<CategoryEntity> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryEntity> categories) {
        this.categories = categories;
    }

    public List<ProductEntity> getProducts() {
        return products;
    }

    public void setProducts(List<ProductEntity> products) {
        this.products = products;
    }
}
