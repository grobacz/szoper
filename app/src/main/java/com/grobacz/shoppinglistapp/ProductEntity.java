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
    public String name;
    public int quantity;
    public int categoryId;
    public long lastModified;

    @Ignore
    public ProductEntity(@NonNull String name, int quantity, int categoryId) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = new Date().getTime();
    }
    
    public ProductEntity(@NonNull String name, int quantity, int categoryId, long lastModified) {
        this.name = name;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.lastModified = lastModified;
    }
}
