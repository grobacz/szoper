package com.grobacz.shoppinglistapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.grobacz.shoppinglistapp.model.SavedState;

import java.util.List;

@Dao
public interface SavedStateDao {
    @Query("SELECT * FROM saved_states ORDER BY timestamp DESC")
    LiveData<List<SavedState>> getAll();

    @Query("SELECT * FROM saved_states WHERE id = :id")
    SavedState getById(int id);

    @Query("SELECT * FROM saved_states WHERE name = :name LIMIT 1")
    SavedState findByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavedState state);

    @Delete
    void delete(SavedState state);

    @Query("DELETE FROM saved_states WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM saved_states")
    void deleteAll();
}
