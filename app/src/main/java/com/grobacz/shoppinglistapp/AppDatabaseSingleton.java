package com.grobacz.shoppinglistapp;

import android.content.Context;

import androidx.room.Room;

public class AppDatabaseSingleton {
    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "shopping_list_database")
                    .allowMainThreadQueries()
                    .addMigrations(
                        AppDatabase.MIGRATION_3_4,
                        AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
    
    public static void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
