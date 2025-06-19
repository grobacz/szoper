package com.grobacz.shoppinglistapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ProductDao {
    @androidx.room.Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity getByName(String name);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);
    
    @Update
    void update(ProductEntity product);

    @Query("SELECT * FROM products ORDER BY lastModified DESC")
    List<ProductEntity> getAll();

    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY lastModified DESC")
    List<ProductEntity> getByCategory(int categoryId);

    @Query("DELETE FROM products WHERE name = :name")
    void deleteByName(String name);
    
    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity loadProductByName(String name);
}
