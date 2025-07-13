package com.grobacz.shoppinglistapp.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    private static final String FILE_PROVIDER_AUTHORITY = "com.grobacz.shoppinglistapp.fileprovider";
    private static final String EXPORT_DIR = "exports";
    private static final String SHARED_DIR = "shared";
    
    /**
     * Creates a temporary file in the app's cache directory.
     * @param context The context
     * @param prefix The prefix of the file name
     * @param extension The file extension (without the dot)
     * @return The created file
     * @throws IOException If the file could not be created
     */
    public static File createTempFile(Context context, String prefix, String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = prefix + "_" + timeStamp + "." + extension;
        
        File storageDir = context.getExternalCacheDir();
        if (storageDir == null) {
            storageDir = context.getCacheDir();
        }
        
        return File.createTempFile(
            fileName,  /* prefix */
            "." + extension,  /* suffix */
            storageDir /* directory */
        );
    }
    
    /**
     * Creates a file in the app's external files directory.
     * @param context The context
     * @param subDir The subdirectory (can be null)
     * @param fileName The name of the file
     * @return The created file
     */
    public static File createExternalFile(Context context, String subDir, String fileName) {
        File storageDir = new File(context.getExternalFilesDir(null), subDir != null ? subDir : "");
        
        // Create the directory if it doesn't exist
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        return new File(storageDir, fileName);
    }
    
    /**
     * Creates a file in the app's shared directory for sharing with other apps.
     * @param context The context
     * @param fileName The name of the file
     * @return The created file
     */
    public static File createSharedFile(Context context, String fileName) {
        return createExternalFile(context, SHARED_DIR, fileName);
    }
    
    /**
     * Creates a file in the app's export directory.
     * @param context The context
     * @param fileName The name of the file
     * @return The created file
     */
    public static File createExportFile(Context context, String fileName) {
        return createExternalFile(context, EXPORT_DIR, fileName);
    }
    
    /**
     * Gets a content URI for a file using FileProvider.
     * @param context The context
     * @param file The file to get the URI for
     * @return The content URI
     */
    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        );
    }
    
    /**
     * Checks if external storage is available for read and write.
     * @return True if external storage is available, false otherwise
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    
    /**
     * Checks if external storage is available to at least read.
     * @return True if external storage is available to read, false otherwise
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
               Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
    
    /**
     * Deletes a directory and all its contents.
     * @param dir The directory to delete
     * @return True if the directory was deleted, false otherwise
     */
    public static boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirectory(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            // The directory is now empty so delete it
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            // If it's a file, delete it
            return dir.delete();
        }
        return false;
    }
    
    /**
     * Gets the MIME type of a file based on its extension.
     * @param file The file
     * @return The MIME type, or "application/octet-stream" if unknown
     */
    public static String getMimeType(File file) {
        String fileName = file.getName();
        String extension = "";
        
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "txt":
                return "text/plain";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "dot":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
            case "xlt":
            case "xla":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
            case "pot":
            case "pps":
            case "ppa":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/x-wav";
            case "mp4":
                return "video/mp4";
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "csv":
                return "text/csv";
            default:
                return "application/octet-stream";
        }
    }
}
