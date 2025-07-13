package com.grobacz.shoppinglistapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for handling SharedPreferences operations.
 */
public class PrefsUtils {

    private static final String PREF_FIRST_RUN = "pref_first_run";
    private static final String PREF_LAST_SYNC = "pref_last_sync";
    private static final String PREF_USER_ID = "pref_user_id";
    private static final String PREF_USER_NAME = "pref_user_name";
    private static final String PREF_USER_EMAIL = "pref_user_email";
    private static final String PREF_REMEMBER_ME = "pref_remember_me";
    private static final String PREF_THEME = "pref_theme";
    private static final String PREF_LANGUAGE = "pref_language";
    private static final String PREF_NOTIFICATIONS = "pref_notifications";
    private static final String PREF_SOUND = "pref_sound";
    private static final String PREF_VIBRATION = "pref_vibration";
    private static final String PREF_BACKUP_ENABLED = "pref_backup_enabled";
    private static final String PREF_LAST_BACKUP = "pref_last_backup";
    private static final String PREF_SYNC_WIFI_ONLY = "pref_sync_wifi_only";
    private static final String PREF_SYNC_INTERVAL = "pref_sync_interval";
    private static final String PREF_RECENT_SEARCHES = "pref_recent_searches";
    private static final String PREF_FAVORITE_ITEMS = "pref_favorite_items";
    private static final String PREF_RECENT_ITEMS = "pref_recent_items";
    private static final String PREF_SORT_ORDER = "pref_sort_order";
    private static final String PREF_VIEW_TYPE = "pref_view_type";
    private static final String PREF_FONT_SIZE = "pref_font_size";
    private static final String PREF_LAST_OPENED_CATEGORY = "pref_last_opened_category";
    
    private static volatile PrefsUtils instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    /**
     * Private constructor to prevent direct instantiation.
     * @param context The application context
     */
    private PrefsUtils(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        this.gson = new Gson();
    }
    
    /**
     * Gets the singleton instance of PrefsUtils.
     * @param context The application context
     * @return The PrefsUtils instance
     */
    public static PrefsUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (PrefsUtils.class) {
                if (instance == null) {
                    instance = new PrefsUtils(context);
                }
            }
        }
        return instance;
    }
    
    // First run methods
    
    public boolean isFirstRun() {
        return prefs.getBoolean(PREF_FIRST_RUN, true);
    }
    
    public void setFirstRun(boolean isFirstRun) {
        prefs.edit().putBoolean(PREF_FIRST_RUN, isFirstRun).apply();
    }
    
    // User-related methods
    
    public String getUserId() {
        return prefs.getString(PREF_USER_ID, null);
    }
    
    public void setUserId(String userId) {
        prefs.edit().putString(PREF_USER_ID, userId).apply();
    }
    
    public String getUserName() {
        return prefs.getString(PREF_USER_NAME, null);
    }
    
    public void setUserName(String userName) {
        prefs.edit().putString(PREF_USER_NAME, userName).apply();
    }
    
    public String getUserEmail() {
        return prefs.getString(PREF_USER_EMAIL, null);
    }
    
    public void setUserEmail(String email) {
        prefs.edit().putString(PREF_USER_EMAIL, email).apply();
    }
    
    public boolean isRememberMe() {
        return prefs.getBoolean(PREF_REMEMBER_ME, false);
    }
    
    public void setRememberMe(boolean rememberMe) {
        prefs.edit().putBoolean(PREF_REMEMBER_ME, rememberMe).apply();
    }
    
    // Theme and appearance
    
    public String getTheme() {
        return prefs.getString(PREF_THEME, "system");
    }
    
    public void setTheme(String theme) {
        prefs.edit().putString(PREF_THEME, theme).apply();
    }
    
    public String getLanguage() {
        return prefs.getString(PREF_LANGUAGE, "");
    }
    
    public void setLanguage(String language) {
        prefs.edit().putString(PREF_LANGUAGE, language).apply();
    }
    
    public float getFontSize() {
        return prefs.getFloat(PREF_FONT_SIZE, 1.0f);
    }
    
    public void setFontSize(float scale) {
        prefs.edit().putFloat(PREF_FONT_SIZE, scale).apply();
    }
    
    // Notifications and sounds
    
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(PREF_NOTIFICATIONS, true);
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_NOTIFICATIONS, enabled).apply();
    }
    
    public boolean isSoundEnabled() {
        return prefs.getBoolean(PREF_SOUND, true);
    }
    
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_SOUND, enabled).apply();
    }
    
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(PREF_VIBRATION, true);
    }
    
    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_VIBRATION, enabled).apply();
    }
    
    // Backup and sync
    
    public boolean isBackupEnabled() {
        return prefs.getBoolean(PREF_BACKUP_ENABLED, true);
    }
    
    public void setBackupEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_BACKUP_ENABLED, enabled).apply();
    }
    
    public long getLastBackupTime() {
        return prefs.getLong(PREF_LAST_BACKUP, 0);
    }
    
    public void setLastBackupTime(long time) {
        prefs.edit().putLong(PREF_LAST_BACKUP, time).apply();
    }
    
    public boolean isSyncWifiOnly() {
        return prefs.getBoolean(PREF_SYNC_WIFI_ONLY, true);
    }
    
    public void setSyncWifiOnly(boolean wifiOnly) {
        prefs.edit().putBoolean(PREF_SYNC_WIFI_ONLY, wifiOnly).apply();
    }
    
    public long getSyncInterval() {
        return prefs.getLong(PREF_SYNC_INTERVAL, 24 * 60 * 60 * 1000); // Default: 24 hours
    }
    
    public void setSyncInterval(long interval) {
        prefs.edit().putLong(PREF_SYNC_INTERVAL, interval).apply();
    }
    
    // Recent searches
    
    public List<String> getRecentSearches() {
        String json = prefs.getString(PREF_RECENT_SEARCHES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> searches = gson.fromJson(json, type);
        return searches != null ? searches : new ArrayList<>();
    }
    
    public void addRecentSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        List<String> searches = getRecentSearches();
        
        // Remove if already exists
        searches.remove(query);
        
        // Add to the beginning of the list
        searches.add(0, query);
        
        // Keep only the last 10 searches
        if (searches.size() > 10) {
            searches = searches.subList(0, 10);
        }
        
        // Save back to preferences
        String json = gson.toJson(searches);
        prefs.edit().putString(PREF_RECENT_SEARCHES, json).apply();
    }
    
    public void clearRecentSearches() {
        prefs.edit().remove(PREF_RECENT_SEARCHES).apply();
    }
    
    // Favorites and recent items
    
    public Set<String> getFavoriteItems() {
        return prefs.getStringSet(PREF_FAVORITE_ITEMS, new HashSet<>());
    }
    
    public void addFavoriteItem(String itemId) {
        Set<String> favorites = new HashSet<>(getFavoriteItems());
        favorites.add(itemId);
        prefs.edit().putStringSet(PREF_FAVORITE_ITEMS, favorites).apply();
    }
    
    public void removeFavoriteItem(String itemId) {
        Set<String> favorites = new HashSet<>(getFavoriteItems());
        favorites.remove(itemId);
        prefs.edit().putStringSet(PREF_FAVORITE_ITEMS, favorites).apply();
    }
    
    public boolean isFavorite(String itemId) {
        return getFavoriteItems().contains(itemId);
    }
    
    public List<String> getRecentItems() {
        String json = prefs.getString(PREF_RECENT_ITEMS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> items = gson.fromJson(json, type);
        return items != null ? items : new ArrayList<>();
    }
    
    public void addRecentItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return;
        }
        
        List<String> items = getRecentItems();
        
        // Remove if already exists
        items.remove(itemId);
        
        // Add to the beginning of the list
        items.add(0, itemId);
        
        // Keep only the last 20 items
        if (items.size() > 20) {
            items = items.subList(0, 20);
        }
        
        // Save back to preferences
        String json = gson.toJson(items);
        prefs.edit().putString(PREF_RECENT_ITEMS, json).apply();
    }
    
    // View settings
    
    public String getSortOrder() {
        return prefs.getString(PREF_SORT_ORDER, "name_asc");
    }
    
    public void setSortOrder(String sortOrder) {
        prefs.edit().putString(PREF_SORT_ORDER, sortOrder).apply();
    }
    
    public String getViewType() {
        return prefs.getString(PREF_VIEW_TYPE, "grid");
    }
    
    public void setViewType(String viewType) {
        prefs.edit().putString(PREF_VIEW_TYPE, viewType).apply();
    }
    
    public String getLastOpenedCategory() {
        return prefs.getString(PREF_LAST_OPENED_CATEGORY, null);
    }
    
    public void setLastOpenedCategory(String categoryId) {
        prefs.edit().putString(PREF_LAST_OPENED_CATEGORY, categoryId).apply();
    }
    
    // Generic methods for any preference
    
    public void putString(String key, @Nullable String value) {
        if (key == null) return;
        prefs.edit().putString(key, value).apply();
    }
    
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        return prefs.getString(key, defValue);
    }
    
    public void putInt(String key, int value) {
        if (key == null) return;
        prefs.edit().putInt(key, value).apply();
    }
    
    public int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }
    
    public void putLong(String key, long value) {
        if (key == null) return;
        prefs.edit().putLong(key, value).apply();
    }
    
    public long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }
    
    public void putFloat(String key, float value) {
        if (key == null) return;
        prefs.edit().putFloat(key, value).apply();
    }
    
    public float getFloat(String key, float defValue) {
        return prefs.getFloat(key, defValue);
    }
    
    public void putBoolean(String key, boolean value) {
        if (key == null) return;
        prefs.edit().putBoolean(key, value).apply();
    }
    
    public boolean getBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }
    
    public void remove(String key) {
        if (key == null) return;
        prefs.edit().remove(key).apply();
    }
    
    public boolean contains(String key) {
        return key != null && prefs.contains(key);
    }
    
    public void clear() {
        prefs.edit().clear().apply();
    }
    
    /**
     * Registers a preference change listener.
     * @param listener The listener to register
     */
    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    
    /**
     * Unregisters a preference change listener.
     * @param listener The listener to unregister
     */
    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
