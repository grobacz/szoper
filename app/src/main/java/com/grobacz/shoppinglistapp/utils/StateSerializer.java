package com.grobacz.shoppinglistapp.utils;

import com.grobacz.shoppinglistapp.model.CategoryEntity;
import com.grobacz.shoppinglistapp.model.ProductEntity;
import com.grobacz.shoppinglistapp.model.SavedState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StateSerializer {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static byte[] serializeState(List<CategoryEntity> categories, List<ProductEntity> products) {
        try {
            ShoppingListState state = new ShoppingListState(categories, products);
            String json = gson.toJson(state);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ShoppingListState deserializeState(byte[] data) throws IOException, ClassNotFoundException {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            return gson.fromJson(json, ShoppingListState.class);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize state: " + e.getMessage(), e);
        }
    }

    public static String stateToJson(ShoppingListState state) {
        try {
            return gson.toJson(state);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ShoppingListState jsonToState(String json) {
        try {
            Type type = new TypeToken<ShoppingListState>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
