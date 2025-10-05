package com.hnguyen703.ui.components;

import com.hnguyen703.utils.Constants;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * Panel displaying summary statistics
 */
public class SummaryPanel extends GridPane {
    
    private final Label lblClasses = new Label("-");
    private final Label lblLOC = new Label("-");
    private final Label lblMethods = new Label("-");
    private final Label lblPackages = new Label("-");
    private final Label lblAvgMPerC = new Label("-");
    private final Label lblAvgLOCPerM = new Label("-");
    private final Label lblAvgAttrPerC = new Label("-");
    private final Label lblMaxParams = new Label("-");
    
    public SummaryPanel() {
        setupLayout();
    }
    
    private void setupLayout() {
        setHgap(12);
        setVgap(6);
        setPadding(new Insets(10));
        
        int row = 0;
        addRow(row++, new Label(Constants.TOTAL_CLASSES_LABEL), lblClasses);
        addRow(row++, new Label(Constants.TOTAL_LOC_LABEL), lblLOC);
        addRow(row++, new Label(Constants.TOTAL_METHODS_LABEL), lblMethods);
        addRow(row++, new Label(Constants.TOTAL_PACKAGES_LABEL), lblPackages);
        addRow(row++, new Label(Constants.AVG_METHODS_PER_CLASS_LABEL), lblAvgMPerC);
        addRow(row++, new Label(Constants.AVG_LOC_PER_METHOD_LABEL), lblAvgLOCPerM);
        addRow(row++, new Label(Constants.AVG_ATTRIBUTES_PER_CLASS_LABEL), lblAvgAttrPerC);
        addRow(row, new Label(Constants.MAX_PARAMS_LABEL), lblMaxParams);
    }
    
    public void updateSummary(int classCount, int lineCount, int methodCount, int packageCount, 
                             int attributeCount, int maxParams) {
        lblClasses.setText(String.valueOf(classCount));
        lblLOC.setText(String.valueOf(lineCount));
        lblMethods.setText(String.valueOf(methodCount));
        lblPackages.setText(String.valueOf(packageCount));
        
        lblAvgMPerC.setText(classCount == 0 ? "0" : 
            String.format("%.2f", (double) methodCount / classCount));
        lblAvgLOCPerM.setText(methodCount == 0 ? "0" : 
            String.format("%.2f", (double) lineCount / methodCount));
        lblAvgAttrPerC.setText(classCount == 0 ? "0" : 
            String.format("%.2f", (double) attributeCount / classCount));
        lblMaxParams.setText(String.valueOf(maxParams));
    }
    
    public void resetSummary() {
        lblClasses.setText("-");
        lblLOC.setText("-");
        lblMethods.setText("-");
        lblPackages.setText("-");
        lblAvgMPerC.setText("-");
        lblAvgLOCPerM.setText("-");
        lblAvgAttrPerC.setText("-");
        lblMaxParams.setText("-");
    }
}