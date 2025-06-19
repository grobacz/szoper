package com.grobacz.shoppinglistapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Room;

@Database(entities = {ProductEntity.class, CategoryEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    // Migration from version 3 to 4: Add lastModified column to products table
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add the new column with a default value of current timestamp
            database.execSQL("ALTER TABLE products ADD COLUMN lastModified INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)");
        }
    };
    
    
    public static void checkDatabase(android.content.Context context) {
        android.database.sqlite.SQLiteDatabase db = null;
        try {
            String path = context.getDatabasePath("shopping_list_database").getPath();
            db = android.database.sqlite.SQLiteDatabase.openDatabase(path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
            android.util.Log.d("AppDatabase", "Database exists at: " + path);
            
            // Check if categories table exists
            android.database.Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'", null);
            boolean hasCategoriesTable = cursor.getCount() > 0;
            cursor.close();
            android.util.Log.d("AppDatabase", "Has categories table: " + hasCategoriesTable);
            
            if (hasCategoriesTable) {
                cursor = db.rawQuery("SELECT COUNT(*) FROM categories", null);
                if (cursor.moveToFirst()) {
                    int count = cursor.getInt(0);
                    android.util.Log.d("AppDatabase", "Number of categories: " + count);
                }
                cursor.close();
            }
        } catch (Exception e) {
            android.util.Log.e("AppDatabase", "Error checking database: " + e.getMessage(), e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    public abstract ProductDao productDao();
    public abstract CategoryDao categoryDao();
    
    // Method to reset the database for testing
    public static void resetDatabase(android.content.Context context) {
        android.util.Log.d("AppDatabase", "Resetting database...");
        context.deleteDatabase("shopping_list_database");
        android.util.Log.d("AppDatabase", "Database reset complete");
    }
}
