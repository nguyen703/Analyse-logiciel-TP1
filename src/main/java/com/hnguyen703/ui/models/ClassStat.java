package com.hnguyen703.ui.models;

/**
 * Model class representing statistics for a class
 */
public class ClassStat {
    public final String name;
    public final int methods;
    public final int attributes;
    
    public ClassStat(String name, int methods, int attributes) {
        this.name = name;
        this.methods = methods;
        this.attributes = attributes;
    }
}