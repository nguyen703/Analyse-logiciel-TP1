package com.hnguyen703.utils;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    /**
     * Opens a directory chooser dialog
     * @param stage The parent stage
     * @param title The dialog title
     * @return Selected directory or null if cancelled
     */
    public static File chooseDirectory(Stage stage, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(stage);
    }
    
    /**
     * Validates if a path is not null or blank
     * @param path The path to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPath(String path) {
        return path != null && !path.isBlank();
    }
}