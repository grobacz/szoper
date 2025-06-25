package com.grobacz.shoppinglistapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position ASC")
    List<CategoryEntity> getAll();

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Query("UPDATE categories SET position = :position WHERE id = :categoryId")
    void updatePosition(int categoryId, int position);

    @Delete
    void delete(CategoryEntity category);
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getCategoryByName(String name);
}
