package com.grobacz.shoppinglistapp;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int position;

    // Room will use this constructor
    public CategoryEntity(int id, String name, int position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    @Ignore
    public CategoryEntity(String name) {
        this(0, name, 0);
    }

    @Ignore
    public CategoryEntity(String name, int position) {
        this(0, name, position);
    }
    
    // Getters and setters
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
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
}
