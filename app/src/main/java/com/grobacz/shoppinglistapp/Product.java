package com.grobacz.shoppinglistapp;

import java.io.Serializable;
import java.util.Date;

public class Product implements Serializable {
    private String name;
    private int quantity;
    private int categoryId;
    private long lastModified;

    public Product(String name, int quantity, int categoryId) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = new Date().getTime();
    }
    
    public Product(String name, int quantity, int categoryId, long lastModified) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    // Conversion from ProductEntity to Product
    public static Product fromEntity(ProductEntity entity) {
        return new Product(entity.name, entity.quantity, entity.categoryId, entity.lastModified);
    }

    // Conversion from Product to ProductEntity
    public ProductEntity toEntity() {
        return new ProductEntity(name, quantity, categoryId, lastModified);
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void updateLastModified() {
        this.lastModified = new Date().getTime();
    }
}
