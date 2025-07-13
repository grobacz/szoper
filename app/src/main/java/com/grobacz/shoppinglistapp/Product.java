package com.grobacz.shoppinglistapp;

import com.grobacz.shoppinglistapp.model.ProductEntity;

import java.io.Serializable;
import java.util.Date;

public class Product implements Serializable {
    private int id;
    private String name;
    private boolean isChecked;
    private int position;
    private int categoryId;
    private long lastModified;

    public Product(String name, boolean isChecked, int position, int categoryId) {
        this.name = name;
        this.isChecked = isChecked;
        this.position = position;
        this.categoryId = categoryId;
        this.lastModified = new Date().getTime();
    }

    public Product(String name, boolean isChecked, int position, int categoryId, long lastModified) {
        this.name = name;
        this.isChecked = isChecked;
        this.position = position;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
    }

    public static Product fromEntity(ProductEntity entity) {
        return new Product(
            entity.getName(),
            entity.isChecked(),
            entity.getPosition(),
            entity.getCategoryId(),
            entity.getLastModified()
        );
    }

    public ProductEntity toEntity() {
        ProductEntity entity = new ProductEntity(
            name,
            isChecked,
            position,
            categoryId
        );
        entity.setId(id);
        entity.setLastModified(lastModified);
        return entity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
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
