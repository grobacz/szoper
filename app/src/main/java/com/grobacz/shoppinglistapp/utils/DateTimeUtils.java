package com.grobacz.shoppinglistapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    
    private static final SimpleDateFormat DATE_TIME_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    
    private static final SimpleDateFormat TIME_FORMAT = 
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }
    
    public static String getRelativeTimeSpanString(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // Convert to seconds
        diff /= 1000;
        
        if (diff < 60) {
            return "Just now";
        }
        
        // Convert to minutes
        diff /= 60;
        if (diff < 60) {
            return diff == 1 ? "1 minute ago" : diff + " minutes ago";
        }
        
        // Convert to hours
        diff /= 60;
        if (diff < 24) {
            return diff == 1 ? "1 hour ago" : diff + " hours ago";
        }
        
        // Convert to days
        diff /= 24;
        if (diff < 7) {
            return diff == 1 ? "Yesterday" : diff + " days ago";
        }
        
        // Return full date for older items
        return formatDate(timestamp);
    }
}
