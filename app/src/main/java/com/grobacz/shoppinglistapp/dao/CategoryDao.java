package com.grobacz.shoppinglistapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.grobacz.shoppinglistapp.model.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);
    
    @Update
    void update(CategoryEntity category);
    
    @Delete
    void delete(CategoryEntity category);
    
    @Query("SELECT * FROM categories ORDER BY position ASC")
    LiveData<List<CategoryEntity>> getAll();
    
    @Query("SELECT * FROM categories WHERE id = :id")
    LiveData<CategoryEntity> getById(int id);
    
    @Query("SELECT * FROM categories ORDER BY position ASC")
    List<CategoryEntity> getAllSync();
    
    @Query("UPDATE categories SET position = :position WHERE id = :id")
    void updatePosition(int id, int position);
    
    @Query("SELECT COUNT(*) FROM categories")
    int getCount();
    
    @Query("DELETE FROM categories")
    void deleteAll();
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getCategoryByName(String name);
    
    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity getByIdSync(int id);
}
