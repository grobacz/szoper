package com.grobacz.shoppinglistapp;

import android.app.Application;

import com.grobacz.shoppinglistapp.AppDatabaseSingleton;

public class ShoppingListApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the database when the application starts
        AppDatabaseSingleton.getInstance(this);
    }
}
