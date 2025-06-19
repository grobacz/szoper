package com.grobacz.shoppinglistapp;

import java.io.Serializable;
import java.util.List;
import com.grobacz.shoppinglistapp.CategoryEntity;
import com.grobacz.shoppinglistapp.Product;

public class SyncData implements Serializable {
    public List<CategoryEntity> categories;
    public List<Product> products;

    public SyncData(List<CategoryEntity> categories, List<Product> products) {
        this.categories = categories;
        this.products = products;
    }
}
