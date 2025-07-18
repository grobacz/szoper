package com.grobacz.shoppinglistapp.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
    tableName = "products",
    foreignKeys = @ForeignKey(
        entity = CategoryEntity.class,
        parentColumns = "id",
        childColumns = "categoryId",
        onDelete = CASCADE
    ),
    indices = {@Index("categoryId")}
)
public class ProductEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int quantity;
    private boolean isChecked;
    private int position;
    private int categoryId;
    private long lastModified;

    public ProductEntity(String name, int quantity, boolean isChecked, int position, int categoryId) {
        this.name = name;
        this.quantity = quantity;
        this.isChecked = isChecked;
        this.position = position;
        this.categoryId = categoryId;
    }

    // Legacy constructor for backward compatibility
    @Ignore
    public ProductEntity(String name, boolean isChecked, int position, int categoryId) {
        this.name = name;
        this.quantity = 1; // Default quantity
        this.isChecked = isChecked;
        this.position = position;
        this.categoryId = categoryId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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
}
