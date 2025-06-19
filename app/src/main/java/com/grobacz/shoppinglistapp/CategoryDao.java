package com.grobacz.shoppinglistapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @androidx.room.Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getByName(String name);

    @androidx.room.Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    CategoryEntity getById(int id);
    @Query("SELECT * FROM categories")
    List<CategoryEntity> getAll();

    @Insert
    void insert(CategoryEntity category);

    @Delete
    void delete(CategoryEntity category);
}
