package com.grobacz.shoppinglistapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "saved_states")
public class SavedState {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private long timestamp;
    private byte[] data;

    public SavedState(String name, long timestamp, byte[] data) {
        this.name = name;
        this.timestamp = timestamp;
        this.data = data;
    }

    // Getters and Setters
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
