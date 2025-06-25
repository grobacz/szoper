package com.grobacz.shoppinglistapp;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "products",
        foreignKeys = @ForeignKey(entity = CategoryEntity.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE),
        indices = {@androidx.room.Index("categoryId")})
public class ProductEntity {
    @PrimaryKey
    @NonNull
    private String name;
    private int quantity;
    private int categoryId;
    private long lastModified;
    private int position;

    // Room will use this constructor
    public ProductEntity(@NonNull String name, int quantity, int categoryId, long lastModified, int position) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
        this.position = position;
    }

    @Ignore
    public ProductEntity(@NonNull String name, int quantity, int categoryId) {
        this(name, quantity, categoryId, System.currentTimeMillis(), 0);
    }

    @Ignore
    public ProductEntity(@NonNull String name, int quantity, int categoryId, long lastModified) {
        this(name, quantity, categoryId, lastModified, 0);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
