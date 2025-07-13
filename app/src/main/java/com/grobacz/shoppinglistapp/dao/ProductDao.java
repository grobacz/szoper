package com.grobacz.shoppinglistapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.grobacz.shoppinglistapp.model.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);
    
    @Update
    void update(ProductEntity product);
    
    @Delete
    void delete(ProductEntity product);
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY position ASC")
    LiveData<List<ProductEntity>> getProductsByCategory(int categoryId);
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY position ASC")
    List<ProductEntity> getProductsByCategorySync(int categoryId);
    
    @Query("SELECT * FROM products")
    List<ProductEntity> getAllProducts();
    
    @Query("SELECT * FROM products WHERE id = :id")
    LiveData<ProductEntity> getById(int id);
    
    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity getByName(String name);
    
    @Query("UPDATE products SET position = :position WHERE id = :id")
    void updatePosition(int id, int position);
    
    @Query("UPDATE products SET categoryId = :categoryId, position = :position WHERE id = :id")
    void updateCategoryAndPosition(int id, int categoryId, int position);
    
    @Query("DELETE FROM products WHERE categoryId = :categoryId")
    void deleteByCategoryId(int categoryId);
    
    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    int getProductCountByCategory(int categoryId);
    
    @Query("DELETE FROM products")
    void deleteAllProducts();
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    List<ProductEntity> getByCategory(int categoryId);
    
    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    int getCountForCategory(int categoryId);
}
