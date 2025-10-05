package com.hnguyen703.utils;

/**
 * Constants used throughout the application
 */
public class Constants {
    public static final String APP_TITLE = "Analyseur de code Java";
    public static final double APP_WIDTH = 1000.0;
    public static final double APP_HEIGHT = 700.0;
    
    // UI Text Constants
    public static final String BROWSE_BUTTON_TEXT = "Parcourir…";
    public static final String ANALYZE_BUTTON_TEXT = "Analyser";
    public static final String PATH_PROMPT_TEXT = "Chemin du dossier projet (src/main/java/...)";
    public static final String PROJECT_FOLDER_LABEL = "Dossier projet:";
    
    // Tab Names
    public static final String STATISTICS_TAB = "Statistiques";
    public static final String CALL_GRAPH_TAB = "Call Graph";
    
    // Summary Labels
    public static final String TOTAL_CLASSES_LABEL = "Classes totales:";
    public static final String TOTAL_LOC_LABEL = "Lignes de code totales:";
    public static final String TOTAL_METHODS_LABEL = "Méthodes totales:";
    public static final String TOTAL_PACKAGES_LABEL = "Packages totaux:";
    public static final String AVG_METHODS_PER_CLASS_LABEL = "Moy. méthodes / classe:";
    public static final String AVG_LOC_PER_METHOD_LABEL = "Moy. LOC / méthode:";
    public static final String AVG_ATTRIBUTES_PER_CLASS_LABEL = "Moy. attributs / classe:";
    public static final String MAX_PARAMS_LABEL = "Max paramètres (toutes méthodes):";
    
    // Table Headers
    public static final String CLASS_COLUMN_HEADER = "Classe";
    public static final String METHODS_COLUMN_HEADER = "Méthodes";
    public static final String ATTRIBUTES_COLUMN_HEADER = "Attributs";
    public static final String QUALIFIED_METHOD_COLUMN_HEADER = "Méthode (qualifiée)";
    public static final String LOC_COLUMN_HEADER = "LOC (lignes de code)";
    
    // Table Section Labels
    public static final String CLASSES_TABLE_LABEL = "Classes (méthodes / attributs)";
    public static final String TOP_METHODS_TABLE_LABEL = "Top 10% méthodes (par LOC)";
    
    // Error Messages
    public static final String SELECT_PROJECT_FOLDER_ERROR = "Veuillez choisir un dossier de projet.";
    public static final String ANALYSIS_ERROR_PREFIX = "Erreur: ";
    
    // Graph Styling
    public static final String GRAPH_STYLESHEET = 
        "node { fill-color: lightblue; size: 20px; text-size: 12px; text-alignment: at-right; }" +
        "edge { arrow-shape: arrow; }";
}