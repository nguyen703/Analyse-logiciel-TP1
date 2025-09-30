package com.hnguyen703;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyzerUI extends Application {

    // Labels de résumé
    private final Label lblClasses   = new Label("-");
    private final Label lblLOC       = new Label("-");
    private final Label lblMethods   = new Label("-");
    private final Label lblPackages  = new Label("-");
    private final Label lblAvgMPerC  = new Label("-");
    private final Label lblAvgLOCPerM= new Label("-");
    private final Label lblAvgAttrPerC=new Label("-");
    private final Label lblMaxParams = new Label("-");

    // Tables
    private final TableView<ClassStat>  tblClasses  = new TableView<>();
    private final TableView<MethodStat> tblMethods  = new TableView<>();

    // Loading indicator
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    @Override
    public void start(Stage stage) {
        // Top bar : chemin + boutons
        TextField pathField = new TextField();
        pathField.setPromptText("Chemin du dossier projet (src/main/java/...)");
        pathField.setMinWidth(300);
        Button browseBtn = new Button("Parcourir…");
        Button runBtn    = new Button("Analyser");

        // Configure loading indicator
        loadingIndicator.setMaxSize(20, 20);
        loadingIndicator.setVisible(false);

        HBox top = new HBox(8, new Label("Dossier projet:"), pathField, browseBtn, runBtn, loadingIndicator);
        top.setPadding(new Insets(10));
        top.setAlignment(Pos.CENTER_LEFT);

        // Résumé (grid 2 colonnes)
        GridPane summary = new GridPane();
        summary.setHgap(12); summary.setVgap(6); summary.setPadding(new Insets(10));
        int r = 0;
        summary.addRow(r++, new Label("Classes totales:"), lblClasses);
        summary.addRow(r++, new Label("Lignes de code totales:"), lblLOC);
        summary.addRow(r++, new Label("Méthodes totales:"), lblMethods);
        summary.addRow(r++, new Label("Packages totaux:"), lblPackages);
        summary.addRow(r++, new Label("Moy. méthodes / classe:"), lblAvgMPerC);
        summary.addRow(r++, new Label("Moy. LOC / méthode:"), lblAvgLOCPerM);
        summary.addRow(r++, new Label("Moy. attributs / classe:"), lblAvgAttrPerC);
        summary.addRow(r, new Label("Max paramètres (toutes méthodes):"), lblMaxParams);

        // Tables
        setupTables();

        VBox root = new VBox(10, top, new Separator(), summary, new Label("Classes (méthodes / attributs)"), tblClasses,
                new Label("Top 10% méthodes (par LOC)"), tblMethods);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);
        stage.setTitle("Analyseur de code Java");
        stage.show();

        // Actions
        browseBtn.setOnAction(ev -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choisir le dossier projet");
            File f = dc.showDialog(stage);
            if (f != null) pathField.setText(f.getAbsolutePath());
        });

        runBtn.setOnAction(ev -> analyzeAndRender(pathField.getText()));
    }

    private void setupTables() {
        // Table classes
        TableColumn<ClassStat, String> cName = new TableColumn<>("Classe");
        cName.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().name));

        TableColumn<ClassStat, Integer> cMeth = new TableColumn<>("Méthodes");
        cMeth.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().methods));

        TableColumn<ClassStat, Integer> cAttr = new TableColumn<>("Attributs");
        cAttr.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().attributes));

        tblClasses.getColumns().add(cName);
        tblClasses.getColumns().add(cMeth);
        tblClasses.getColumns().add(cAttr);
        tblClasses.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Table méthodes (top 10% LOC)
        TableColumn<MethodStat, String> mQN = new TableColumn<>("Méthode (qualifiée)");
        mQN.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().qualifiedName));

        TableColumn<MethodStat, Integer> mLOC = new TableColumn<>("LOC (lignes de code)");
        mLOC.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().loc));

        tblMethods.getColumns().add(mQN);
        tblMethods.getColumns().add(mLOC);
        tblMethods.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void analyzeAndRender(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez choisir un dossier de projet.").showAndWait();
            return;
        }
        loadingIndicator.setVisible(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                try {
                    Analyzer a = new Analyzer();
                    a.analyze(projectPath, false);

                    // Résumé
                    Platform.runLater(() -> {
                        lblClasses.setText(String.valueOf(a.getClassCount()));
                        lblLOC.setText(String.valueOf(a.getLineCount()));
                        lblMethods.setText(String.valueOf(a.getMethodCount()));
                        lblPackages.setText(String.valueOf(a.getPackageCount()));
                        lblAvgMPerC.setText(a.getClassCount() == 0 ? "0" : String.format("%.2f", (double) a.getMethodCount() / a.getClassCount()));
                        lblAvgLOCPerM.setText(a.getMethodCount() == 0 ? "0" : String.format("%.2f", (double) a.getLineCount() / a.getMethodCount()));
                        lblAvgAttrPerC.setText(a.getClassCount() == 0 ? "0" : String.format("%.2f", (double) a.getAttributeCount() / a.getClassCount()));
                        lblMaxParams.setText(String.valueOf(a.getMaxParams()));

                        // Classes (méthodes / attributs)
                        Map<String,Integer> mpc = a.getMethodsPerClass();
                        Map<String,Integer> apc = a.getAttributesPerClass();
                        List<ClassStat> classRows = mpc.keySet().stream()
                                .map(k -> new ClassStat(k, mpc.getOrDefault(k, 0), apc.getOrDefault(k, 0)))
                                .sorted(Comparator.comparingInt((ClassStat cs) -> cs.methods).reversed())
                                .collect(Collectors.toList());
                        tblClasses.setItems(FXCollections.observableArrayList(classRows));

                        // Top 10% méthodes par LOC
                        Map<String,Integer> lpm = a.getLinesPerMethod();
                        int topN = Math.max(1, (int) Math.ceil(lpm.size() * 0.1));
                        List<MethodStat> methodRows = lpm.entrySet().stream()
                                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                                .limit(topN)
                                .map(e -> new MethodStat(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                        tblMethods.setItems(FXCollections.observableArrayList(methodRows));

                        loadingIndicator.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    // Petites classes pour les tables
    public static class ClassStat {
        public final String name;
        public final int methods;
        public final int attributes;
        public ClassStat(String n, int m, int a) { this.name = n; this.methods = m; this.attributes = a; }
    }

    public static class MethodStat {
        public final String qualifiedName;
        public final int loc;
        public MethodStat(String n, int l) { this.qualifiedName = n; this.loc = l; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}