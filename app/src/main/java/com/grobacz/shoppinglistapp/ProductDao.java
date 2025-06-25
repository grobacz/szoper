package com.grobacz.shoppinglistapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY position ASC, lastModified DESC")
    List<ProductEntity> getByCategory(int categoryId);

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);

    @Update
    void update(ProductEntity product);

    @Query("UPDATE products SET position = :position WHERE name = :productName")
    void updatePosition(String productName, int position);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM products WHERE categoryId = :categoryId")
    int getNextPositionForCategory(int categoryId);

    @Delete
    void delete(ProductEntity product);

    @Query("DELETE FROM products WHERE name = :name")
    void deleteByName(String name);
    
    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity loadProductByName(String name);
    
    @Query("SELECT * FROM products ORDER BY categoryId, position")
    List<ProductEntity> getAllProducts();
}
