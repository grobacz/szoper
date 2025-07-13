package com.grobacz.shoppinglistapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Room;

import com.grobacz.shoppinglistapp.dao.CategoryDao;
import com.grobacz.shoppinglistapp.dao.ProductDao;
import com.grobacz.shoppinglistapp.dao.SavedStateDao;
import com.grobacz.shoppinglistapp.model.CategoryEntity;
import com.grobacz.shoppinglistapp.model.ProductEntity;
import com.grobacz.shoppinglistapp.model.SavedState;

@Database(entities = {ProductEntity.class, CategoryEntity.class, SavedState.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    // Migration from version 3 to 4: Add lastModified column to products table
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add the new column with a default value of current timestamp
            database.execSQL("ALTER TABLE products ADD COLUMN lastModified INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)");
        }
    };
    
    // Migration from version 4 to 5: Add position column to categories table
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add position column with default value 0
            database.execSQL("ALTER TABLE categories ADD COLUMN position INTEGER NOT NULL DEFAULT 0");
        }
    };
    
    // Migration from version 5 to 6: Add position column to products table
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add position column with default value 0
            database.execSQL("ALTER TABLE products ADD COLUMN position INTEGER NOT NULL DEFAULT 0");
            // Update existing products to have unique positions
            database.execSQL("UPDATE products SET position = (SELECT COUNT(*) FROM products p2 WHERE p2.rowid <= products.rowid) - 1");
        }
    };
    
    // Migration from version 6 to 7: Add saved_states table
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create saved_states table
            database.execSQL("CREATE TABLE IF NOT EXISTS saved_states (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "data BLOB)");
        }
    };
    
    // Migration from version 7 to 8: Ensure position column exists in categories table
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // This migration ensures the position column exists with a default value of 0
            database.execSQL("ALTER TABLE categories ADD COLUMN position INTEGER NOT NULL DEFAULT 0");
        }
    };
    
    // Migration from version 8 to 9: Remove color column from categories table
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // SQLite doesn't support DROP COLUMN, so we need to recreate the table
            // 1. Create new table without color column
            database.execSQL("CREATE TABLE categories_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "position INTEGER NOT NULL DEFAULT 0)");
            
            // 2. Copy data from old table (excluding color column)
            database.execSQL("INSERT INTO categories_new (id, name, position) " +
                    "SELECT id, name, position FROM categories");
            
            // 3. Drop old table
            database.execSQL("DROP TABLE categories");
            
            // 4. Rename new table to original name
            database.execSQL("ALTER TABLE categories_new RENAME TO categories");
        }
    };
    
    // Migration from version 9 to 10: Add quantity column to products table
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add quantity column with default value of 1
            database.execSQL("ALTER TABLE products ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
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
    public abstract SavedStateDao savedStateDao();
    
    // Method to reset the database for testing
    public static void resetDatabase(android.content.Context context) {
        android.util.Log.d("AppDatabase", "Resetting database...");
        context.deleteDatabase("shopping_list_database");
        // Reset the singleton instance
        AppDatabaseSingleton.resetInstance();
        android.util.Log.d("AppDatabase", "Database reset complete");
    }
    
    /**
     * Gets the singleton instance of AppDatabase.
     * @param context The application context
     * @return The singleton database instance
     */
    public static AppDatabase getDatabase(final android.content.Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "shopping_list_database")
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration() // This will clear the database on version mismatch
                    .build();
    }
    
    /**
     * Gets the singleton instance of AppDatabase using the singleton pattern.
     * @param context The application context
     * @return The singleton database instance
     */
    public static AppDatabase getInstance(final android.content.Context context) {
        return AppDatabaseSingleton.getInstance(context);
    }
}
