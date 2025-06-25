package com.grobacz.shoppinglistapp;

import java.io.Serializable;
import java.util.Date;

public class Product implements Serializable {
    private String name;
    private int quantity;
    private int categoryId;
    private long lastModified;
    private int position;

    public Product(String name, int quantity, int categoryId) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = new Date().getTime();
        this.position = 0;
    }

    public Product(String name, int quantity, int categoryId, long lastModified) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
        this.position = 0;
    }

    public Product(String name, int quantity, int categoryId, long lastModified, int position) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
        this.position = position;
    }

    public static Product fromEntity(ProductEntity entity) {
        return new Product(
            entity.getName(), 
            entity.getQuantity(), 
            entity.getCategoryId(), 
            entity.getLastModified(), 
            entity.getPosition()
        );
    }

    public ProductEntity toEntity() {
        ProductEntity entity = new ProductEntity(
            name, 
            quantity, 
            categoryId, 
            lastModified, 
            position
        );
        return entity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public void updateLastModified() {
        this.lastModified = new Date().getTime();
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
        updateLastModified();
    }
}
