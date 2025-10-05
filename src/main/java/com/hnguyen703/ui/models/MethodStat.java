package com.hnguyen703.ui.models;

/**
 * Model class representing statistics for a method
 */
public class MethodStat {
    public final String qualifiedName;
    public final int loc;
    
    public MethodStat(String qualifiedName, int loc) {
        this.qualifiedName = qualifiedName;
        this.loc = loc;
    }
}